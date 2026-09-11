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
import androidx.compose.material3.MaterialTheme
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
import com.tango.recall.data.Note
import com.tango.recall.ui.AppViewModel
import kotlin.math.roundToInt

@Composable
fun StatsScreen(vm: AppViewModel, nav: NavController) {
    var weakest by remember { mutableStateOf<List<Pair<Note, Double>>>(emptyList()) }
    LaunchedEffect(Unit) {
        vm.loadStats()
        weakest = vm.weakest()
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

            if (stats.hardest.isNotEmpty()) {
                item {
                    SectionTitle("何度も間違えているもの")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "繰り返し忘れる項目は、覚え方そのものを変えたほうが早いことが多いです。" +
                            "語源や対になる語と結び付けてみてください。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                items(stats.hardest, key = { it.first.id }) { (note, lapses) ->
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
                            Pill(
                                "$lapses 回",
                                MaterialTheme.colorScheme.errorContainer,
                                MaterialTheme.colorScheme.onErrorContainer,
                            )
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
