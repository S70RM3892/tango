package com.tango.recall.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.tango.recall.data.Note
import com.tango.recall.data.ReadingProgress
import com.tango.recall.data.ReadingRecord
import com.tango.recall.data.wordCount
import com.tango.recall.data.wordsPerMinute
import com.tango.recall.ui.AppViewModel
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * 速読.
 *
 * The one thing here that recall practice cannot train. Reading speed only moves when
 * it is measured, and it has to be measured against a passage long enough for the eye
 * to settle — and always next to a comprehension check, because otherwise the fastest
 * way to a better number is to stop reading.
 *
 * A rate is only counted towards the average when the passage was understood, for the
 * same reason.
 */
@Composable
fun ReadingScreen(vm: AppViewModel, nav: NavController) {
    var stage by remember { mutableStateOf<Stage>(Stage.Index) }
    var passages by remember { mutableStateOf<List<Note>?>(null) }
    var progress by remember { mutableStateOf<ReadingProgress?>(null) }
    var lastByNote by remember { mutableStateOf<Map<Long, ReadingRecord>>(emptyMap()) }
    var reload by remember { mutableStateOf(0) }

    LaunchedEffect(reload) {
        val list = vm.readingPassages()
        passages = list
        progress = vm.readingProgress()
        lastByNote = list.mapNotNull { note -> vm.lastReading(note.id)?.let { note.id to it } }.toMap()
    }

    val title = when (stage) {
        is Stage.Index -> "速読"
        is Stage.Reading -> "読む"
        else -> "内容の確認"
    }

    Scaffold(
        topBar = {
            TangoTopBar(title, onBack = {
                if (stage is Stage.Index) nav.popBackStack() else stage = Stage.Index
            })
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val current = stage) {
                is Stage.Index -> {
                    val list = passages
                    when {
                        list == null -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                            CircularProgressIndicator()
                        }

                        list.isEmpty() -> Box(Modifier.fillMaxSize().padding(24.dp), Alignment.Center) {
                            Text(
                                "速読用の英文がありません。ノート型「速読」で英文・設問・全訳を入れると、" +
                                    "ここで時間を測って読めます。",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }

                        else -> PassageIndex(
                            passages = list,
                            progress = progress,
                            lastByNote = lastByNote,
                            onRead = { stage = Stage.Reading(it, System.currentTimeMillis()) },
                        )
                    }
                }

                is Stage.Reading -> ReadingPane(
                    note = current.note,
                    startedAt = current.startedAt,
                    onDone = { took -> stage = Stage.Question(current.note, took) },
                )

                is Stage.Question -> QuestionPane(
                    note = current.note,
                    tookMs = current.tookMs,
                    onAnswered = { understood ->
                        vm.recordReading(
                            noteId = current.note.id,
                            tookMs = current.tookMs,
                            words = wordCount(current.note["passage"]),
                            understood = understood,
                        ) { reload++ }
                        stage = Stage.Result(current.note, current.tookMs, understood)
                    },
                )

                is Stage.Result -> ResultPane(
                    note = current.note,
                    tookMs = current.tookMs,
                    understood = current.understood,
                    onOpen = { nav.navigate(com.tango.recall.Routes.note(current.note.id, current.note.deckId)) },
                    onDone = { stage = Stage.Index },
                )
            }
        }
    }
}

private sealed interface Stage {
    data object Index : Stage
    data class Reading(val note: Note, val startedAt: Long) : Stage
    data class Question(val note: Note, val tookMs: Long) : Stage
    data class Result(val note: Note, val tookMs: Long, val understood: Boolean) : Stage
}

/**
 * 120 words a minute.
 *
 * The Common Test's reading paper runs to roughly 5,700–6,300 words in 80 minutes, and
 * a good part of that time goes on the questions rather than the text, so the passages
 * themselves have to move at about this rate.
 */
private const val TARGET_WPM = 120

@Composable
private fun PassageIndex(
    passages: List<Note>,
    progress: ReadingProgress?,
    lastByNote: Map<Long, ReadingRecord>,
    onRead: (Note) -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatTile(progress?.meanWordsPerMinute?.toString() ?: "—", "平均 語/分")
                    StatTile(progress?.bestWordsPerMinute?.toString() ?: "—", "最高")
                    StatTile(
                        progress?.understoodRate?.let { "${(it * 100).roundToInt()}%" } ?: "—",
                        "理解できた割合",
                    )
                    StatTile("${progress?.sessions ?: 0}", "回数")
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "共通テストのリーディングは80分で 5,700〜6,300 語ほどあり、設問を解く時間を引くと、" +
                        "本文は毎分 $TARGET_WPM 語くらいで読める必要があります。" +
                        "内容が取れなかった回は平均に入れません。速いだけの読みは意味がないからです。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                progress?.recent?.takeIf { it.size >= 2 }?.let { recent ->
                    Spacer(Modifier.height(12.dp))
                    SpeedTrend(recent.reversed())
                }
            }
        }

        items(passages, key = { it.id }) { note ->
            val words = wordCount(note["passage"])
            SectionCard {
                Text(note.title(), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    buildString {
                        append("$words 語 ・ 目安 ${(words * 60.0 / TARGET_WPM).roundToInt()} 秒")
                        lastByNote[note.id]?.let { append(" ・ 前回 ${it.wordsPerMinute} 語/分") }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = { onRead(note) }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (lastByNote.containsKey(note.id)) "もう一度読む" else "読む")
                }
            }
        }
    }
}

/** The last few rates, oldest on the left. */
@Composable
private fun SpeedTrend(records: List<ReadingRecord>) {
    val peak = (records.maxOfOrNull { it.wordsPerMinute } ?: 1).coerceAtLeast(TARGET_WPM)
    Row(
        Modifier.fillMaxWidth().height(72.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        records.forEach { record ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .width(14.dp)
                        .height((6 + 54 * record.wordsPerMinute / peak).dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (record.understood) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant
                        ),
                )
            }
        }
    }
    Text(
        "棒は1回ごとの速さ。薄い棒は内容が取れなかった回です。",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ReadingPane(note: Note, startedAt: Long, onDone: (Long) -> Unit) {
    var elapsed by remember(startedAt) { mutableStateOf(0L) }
    LaunchedEffect(startedAt) {
        while (true) {
            elapsed = System.currentTimeMillis() - startedAt
            delay(250)
        }
    }

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),
        ) {
            Text(note.title(), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            Text(
                note["passage"],
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = MaterialTheme.typography.bodyLarge.fontSize * 1.6,
            )
            Spacer(Modifier.height(24.dp))
        }
        HorizontalDivider()
        Column(Modifier.padding(16.dp)) {
            Text(
                "${elapsed / 1000} 秒　戻り読みをせず、最後まで一度で読み切ってください。",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { onDone(System.currentTimeMillis() - startedAt) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("読み終えた") }
        }
    }
}

@Composable
private fun QuestionPane(note: Note, tookMs: Long, onAnswered: (Boolean) -> Unit) {
    var revealed by remember(note.id) { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text("設問", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        Text(note["question"], style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        Text(
            "本文には戻らず、覚えている範囲で答えを頭の中で作ってから開いてください。",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))

        if (!revealed) {
            Button(onClick = { revealed = true }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text("答えを見る")
            }
        } else {
            SectionCard {
                LabeledValue("答え", note["answer"])
                val points = note["points"].lines().map { it.trim() }.filter { it.isNotEmpty() }
                if (points.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    LabeledValue("押さえる点", points.joinToString("\n") { "・$it" })
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "${wordCount(note["passage"])} 語を ${tookMs / 1000} 秒で読みました。",
                style = MaterialTheme.typography.labelMedium,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onAnswered(true) }, modifier = Modifier.weight(1f)) { Text("取れていた") }
                OutlinedButton(onClick = { onAnswered(false) }, modifier = Modifier.weight(1f)) {
                    Text("取れていなかった")
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ResultPane(
    note: Note,
    tookMs: Long,
    understood: Boolean,
    onOpen: () -> Unit,
    onDone: () -> Unit,
) {
    val words = wordCount(note["passage"])
    val wpm = wordsPerMinute(words, tookMs)
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("$wpm", style = MaterialTheme.typography.displayLarge)
        Text("語 / 分", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        Text(
            when {
                !understood -> "速さは記録しますが、平均には入れません。取れていない読みは速くても意味がないからです。"
                wpm >= TARGET_WPM -> "目安の $TARGET_WPM 語/分を超えています。この速さで内容が取れているなら十分です。"
                else -> "目安は $TARGET_WPM 語/分です。まずは同じ英文をもう一度、少し速く読んでみてください。"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("一覧に戻る") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onOpen, modifier = Modifier.fillMaxWidth()) { Text("全訳を読む") }
    }
}
