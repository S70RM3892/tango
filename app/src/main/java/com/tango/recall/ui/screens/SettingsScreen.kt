package com.tango.recall.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.material3.AlertDialog
import androidx.navigation.NavController
import com.tango.recall.ui.AppViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(vm: AppViewModel, nav: NavController) {
    var retention by remember { mutableStateOf(vm.desiredRetention.toFloat()) }
    var maxReviews by remember { mutableStateOf(vm.maxReviewsPerDay.toFloat()) }
    var showRelated by remember { mutableStateOf(vm.showRelated) }
    var pickingDate by remember { mutableStateOf(false) }
    var pickingTime by remember { mutableStateOf(false) }
    var reminder by remember { mutableStateOf(vm.reminderEnabled) }
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Android 13 and later will not show a notification until it has been allowed.
    val askNotifications = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        reminder = granted
        vm.setReminder(granted)
        if (!granted) vm.toast = "通知が許可されていないため、リマインダーは鳴りません"
    }

    fun enableReminder() {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            askNotifications.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            reminder = true
            vm.setReminder(true)
        }
    }

    LaunchedEffect(Unit) { vm.loadExamOutlook() }
    LaunchedEffect(vm.toast) { vm.toast?.let { snackbar.showSnackbar(it); vm.toast = null } }

    val backupExport = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { vm.exportBackup(it) } }

    val backupImport = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { vm.importBackup(it) } }

    Scaffold(
        topBar = { TangoTopBar("設定", onBack = { nav.popBackStack() }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionCard {
                SectionTitle("試験日")
                Spacer(Modifier.height(4.dp))
                Text(
                    if (vm.examDate > 0) formatExamDate(vm.examDate) else "設定されていません",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "設定すると、試験日の時点で覚えていられるかどうかで復習を組み立てます。" +
                        "残り60日を切ると目標定着率を自動で ${(com.tango.recall.data.Repository.EXAM_PEAK_RETENTION * 100).toInt()}% まで" +
                        "少しずつ引き上げ、間隔を詰めていきます。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                vm.examOutlook?.let { outlook ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "いまの目標定着率 ${(outlook.effectiveRetention * 100).roundToInt()}%" +
                            "（試験まで ${outlook.daysLeft} 日）",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = { pickingDate = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (vm.examDate > 0) "試験日を変更" else "試験日を設定")
                }
                if (vm.examDate > 0) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { vm.setExamDate(0L) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("試験日を解除") }
                }
            }

            SectionCard {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionTitle("毎日の通知")
                    Switch(
                        checked = reminder,
                        onCheckedChange = { on ->
                            if (on) enableReminder() else { reminder = false; vm.setReminder(false) }
                        },
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "決めた時刻に、その日に残っている枚数を知らせます。" +
                        "残っていない日は鳴りません（鳴らない日があるほうが、通知は効きます）。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (reminder) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { pickingTime = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("通知する時刻：%02d:%02d".format(vm.reminderHour, vm.reminderMinute)) }
                }
            }

            SectionCard {
                SectionTitle("目標とする定着率：${(retention * 100).roundToInt()}%")
                Slider(
                    value = retention,
                    onValueChange = { retention = it },
                    onValueChangeFinished = { vm.setDesiredRetention(retention.toDouble()) },
                    valueRange = 0.75f..0.97f,
                )
                Text(
                    "復習のタイミングを決める値です。高くすると忘れにくくなりますが、" +
                        "復習の回数が増えます。試験が近いときだけ上げる、という使い方もできます。" +
                        "特に理由がなければ 90% のままで構いません。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SectionCard {
                SectionTitle("1日の復習上限：${maxReviews.toInt()} 枚")
                Slider(
                    value = maxReviews,
                    onValueChange = { maxReviews = it },
                    onValueChangeFinished = { vm.setMaxReviews(maxReviews.toInt()) },
                    valueRange = 20f..500f,
                )
            }

            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        SectionTitle("答えたあとに「つながり」を出す")
                        Text(
                            "関係づけた語や物質を、答え合わせの直後にまとめて見せます。",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = showRelated,
                        onCheckedChange = { showRelated = it; vm.setShowRelated(it) },
                    )
                }
            }

            SectionCard {
                SectionTitle("バックアップ")
                Spacer(Modifier.height(4.dp))
                Text(
                    "デッキ・ノート・関係・学習の進み具合をまとめて1つの JSON に書き出します。" +
                        "機種変更のときはこれを読み込めば続きから学習できます。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { backupExport.launch("tango-backup.json") },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("書き出す") }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { backupImport.launch(arrayOf("application/json", "text/*")) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("読み込む（今の内容は置き換わります）") }
            }

            SectionCard {
                SectionTitle("過去問を入れる")
                Spacer(Modifier.height(4.dp))
                Text(
                    "京都大学は、一般選抜の試験問題と「出題意図」を公式サイトで公開しています" +
                        "（英語は III・IV のみ。許諾の得られない英文は掲載されません）。" +
                        "同梱の数学は出題の型に沿った自作問題なので、実物はここから写して" +
                        "「数学」のノートに足してください。出題意図は、このアプリが問う「方針」" +
                        "そのものなので、ノートの『押さえる手順』にそのまま使えます。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { openUrl(context, KYOTO_PAST_PAPERS) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("京都大学の試験問題・出題意図を開く") }
                Spacer(Modifier.height(8.dp))
                Text(
                    "ノートの「出典」欄に URL を書いておくと、今日の1問からそのまま開けます。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SectionCard {
                SectionTitle("覚え方について")
                Spacer(Modifier.height(8.dp))
                Text(
                    "・思い出そうとする負荷そのものが記憶を強くします。答えを見る前に、必ず数秒考えてください。\n" +
                        "・「和 → 英（入力）」のように書いて答える向きは、見て思い出すだけより強く残ります。\n" +
                        "・同じデッキを続けて回すより、複数のデッキを混ぜたほうが定着します（ホームの「すべてまとめて学習」）。\n" +
                        "・間違えた項目は、語源・対義語・反応の相手と結び付け直すと抜けにくくなります。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (pickingDate) {
        ExamDatePicker(
            initial = vm.examDate,
            onDismiss = { pickingDate = false },
            onPick = { picked -> pickingDate = false; vm.setExamDate(picked) },
        )
    }

    if (pickingTime) {
        ReminderTimePicker(
            hour = vm.reminderHour,
            minute = vm.reminderMinute,
            onDismiss = { pickingTime = false },
            onPick = { h, m -> pickingTime = false; vm.setReminder(true, h, m) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimePicker(
    hour: Int,
    minute: Int,
    onDismiss: () -> Unit,
    onPick: (Int, Int) -> Unit,
) {
    val state = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onPick(state.hour, state.minute) }) { Text("決定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("やめる") } },
        text = { TimePicker(state) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExamDatePicker(initial: Long, onDismiss: () -> Unit, onPick: (Long) -> Unit) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.takeIf { it > 0 } ?: System.currentTimeMillis(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { state.selectedDateMillis?.let { onPick(toLocalExamMoment(it)) } },
                enabled = state.selectedDateMillis != null,
            ) { Text("決定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("やめる") } },
    ) { DatePicker(state) }
}

/**
 * The picker hands back UTC midnight; treat it as the morning of that day locally, so
 * "days until the exam" does not come out a day off either side of the date line.
 */
private fun toLocalExamMoment(utcMidnight: Long): Long {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMidnight }
    return Calendar.getInstance().apply {
        clear()
        set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH), 9, 0, 0)
    }.timeInMillis
}

private fun formatExamDate(millis: Long): String =
    SimpleDateFormat("yyyy年M月d日", Locale.JAPAN).format(millis)

/**
 * Where the real papers are.
 *
 * The university publishes the questions and, more usefully, its own statement of what
 * each question was asking for — which is the same thing this app makes you write down
 * as the 方針. Linking is the honest way to give access to them: the questions are
 * copyrighted works, and the page says in as many words that publishing them is not
 * permission to reproduce them.
 */
private const val KYOTO_PAST_PAPERS = "https://www.kyoto-u.ac.jp/ja/admissions/undergrad/past-eq"
