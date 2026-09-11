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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.tango.recall.ui.AppViewModel
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(vm: AppViewModel, nav: NavController) {
    var retention by remember { mutableStateOf(vm.desiredRetention.toFloat()) }
    var maxReviews by remember { mutableStateOf(vm.maxReviewsPerDay.toFloat()) }
    var showRelated by remember { mutableStateOf(vm.showRelated) }
    val snackbar = remember { SnackbarHostState() }

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
}
