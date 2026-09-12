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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.tango.recall.Routes
import com.tango.recall.data.ConfusionPair
import com.tango.recall.data.Leech
import com.tango.recall.data.Note
import com.tango.recall.ui.AppViewModel
import kotlin.math.roundToInt

@Composable
fun StatsScreen(vm: AppViewModel, nav: NavController) {
    var weakest by remember { mutableStateOf<List<Pair<Note, Double>>>(emptyList()) }
    var confusions by remember { mutableStateOf<List<ConfusionPair>>(emptyList()) }
    var leeches by remember { mutableStateOf<List<Leech>>(emptyList()) }
    var reload by remember { mutableStateOf(0) }
    LaunchedEffect(reload) {
        vm.loadStats()
        weakest = vm.weakest()
        confusions = vm.confusionPairs()
        leeches = vm.leeches()
    }
    val stats = vm.stats

    Scaffold(topBar = { TangoTopBar("学習の状況", onBack = { nav.popBackStack() }) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (stats == null) {
                item { Text("集計中…") }
                return@LazyColumn
            }

            item {
                SectionCard {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        StatTile("${stats.reviewsToday}", "今日の解答")
                        StatTile(
                            if (stats.reviewsToday == 0) "—"
                            else "${stats.correctToday * 100 / stats.reviewsToday}%",
                            "今日の正答率",
                        )
                        StatTile("${stats.streakDays}", "連続日数")
                    }
                }
            }

            item {
                SectionCard {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        StatTile("${stats.totalNotes}", "ノート")
                        StatTile("${stats.totalCards}", "カード")
                        StatTile("${stats.matureCards}", "定着済み")
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "「定着済み」は次回まで3週間以上あけられるカードです。" +
                            "平均の記憶保持期間は約 ${stats.averageStability.roundToInt()} 日。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                SectionCard {
                    SectionTitle("これから7日間の復習予定")
                    Spacer(Modifier.height(12.dp))
                    Forecast(stats.dueNext7Days)
                }
            }

            if (weakest.isNotEmpty()) {
                item {
                    SectionTitle("そろそろ忘れそうなもの")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "予測される思い出しやすさが低い順。復習すると一番効果が大きいところです。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                items(weakest, key = { it.first.id }) { (note, r) ->
                    SectionCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(note.title(), style = MaterialTheme.typography.titleMedium)
                                Text(
                                    note.subtitle(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                )
                            }
                            Pill("${(r * 100).roundToInt()}%")
                        }
                    }
                }
            }

            if (confusions.isNotEmpty()) {
                item {
                    SectionTitle("取り違えた組")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "答えを書き間違えたとき、それが別のノートの答えだった回数です。" +
                            "2回以上まちがえた組は自動で「混同注意」でつながります。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                items(confusions, key = { it.a.id * 1_000_003 + it.b.id }) { pair ->
                    SectionCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "${pair.a.title()} ⇄ ${pair.b.title()}",
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    if (pair.linked) "「混同注意」でつながっています"
                                    else "まだつながっていません",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Pill(
                                "${pair.times} 回",
                                MaterialTheme.colorScheme.tertiaryContainer,
                                MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                        }
                    }
                }
            }

            if (leeches.isNotEmpty()) {
                item {
                    SectionTitle("つまずき続けているカード")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "同じカードを何度も落としているときは、回数を重ねても抜けません。" +
                            "覚え方そのものを変えるほうが早いです — 語源や対になるものと結ぶ、" +
                            "欄を分けて問いを小さくする、いったん保留にして後から戻す。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                items(leeches, key = { it.card.id }) { leech ->
                    SectionCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(leech.note.title(), style = MaterialTheme.typography.titleMedium)
                                Text(
                                    leech.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                )
                            }
                            Pill(
                                "もう一度 ${leech.misses} 回",
                                MaterialTheme.colorScheme.errorContainer,
                                MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = { nav.navigate(Routes.note(leech.note.id, leech.note.deckId)) },
                                modifier = Modifier.weight(1f),
                            ) { Text("ノートを直す") }
                            OutlinedButton(
                                onClick = { vm.suspendCard(leech.card.id) { reload++ } },
                                enabled = !leech.card.suspended,
                                modifier = Modifier.weight(1f),
                            ) { Text(if (leech.card.suspended) "保留中" else "保留する") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Forecast(counts: List<Int>) {
    val labels = listOf("今日", "明日", "2日後", "3日後", "4日後", "5日後", "6日後")
    val peak = (counts.maxOrNull() ?: 0).coerceAtLeast(1)
    Row(
        Modifier.fillMaxWidth().height(140.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        counts.forEachIndexed { i, n ->
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                Text("$n", style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier
                        .width(20.dp)
                        .height((8 + 96 * n / peak).dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primary),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    labels.getOrElse(i) { "" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
