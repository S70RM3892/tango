package com.tango.recall.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.tango.recall.Routes
import com.tango.recall.data.Deck
import com.tango.recall.data.DeckCounts
import com.tango.recall.ui.AppViewModel

@Composable
fun HomeScreen(vm: AppViewModel, nav: NavController) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(vm.toast) {
        vm.toast?.let { snackbar.showSnackbar(it); vm.toast = null }
    }

    val totalStudyable = vm.counts.values.sumOf { it.studyable }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TangoTopBar("Tango") {
                IconButton(onClick = { nav.navigate(Routes.browse(null)) }) {
                    Icon(Icons.Default.Search, contentDescription = "検索")
                }
                IconButton(onClick = { nav.navigate(Routes.STATS) }) {
                    Icon(AppIcons.BarChart, contentDescription = "統計")
                }
                IconButton(onClick = { nav.navigate(Routes.SETTINGS) }) {
                    Icon(Icons.Default.Settings, contentDescription = "設定")
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { nav.navigate(Routes.deck(0L)) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("デッキを追加") },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SectionCard {
                    Text("今日の学習", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (totalStudyable > 0)
                            "$totalStudyable 枚が待っています。デッキをまたいで交互に出題します。"
                        else "今日の分は終わりました。お疲れさま。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { nav.navigate(Routes.review(null)) },
                        enabled = totalStudyable > 0,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("すべてまとめて学習") }
                }
            }

            item { SectionTitle("デッキ") }

            if (vm.decks.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "まだデッキがありません。\n右下のボタンから作成してください。",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            items(vm.decks, key = { it.id }) { deck ->
                DeckRow(
                    deck = deck,
                    counts = vm.counts[deck.id] ?: DeckCounts(0, 0, 0, 0),
                    onStudy = { nav.navigate(Routes.review(deck.id)) },
                    onBrowse = { nav.navigate(Routes.browse(deck.id)) },
                    onEdit = { nav.navigate(Routes.deck(deck.id)) },
                )
            }
        }
    }
}

@Composable
private fun DeckRow(
    deck: Deck,
    counts: DeckCounts,
    onStudy: () -> Unit,
    onBrowse: () -> Unit,
    onEdit: () -> Unit,
) {
    SectionCard(modifier = Modifier.clickable(onClick = onStudy)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(deck.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${deck.noteType.label} ・ 全 ${counts.total} 枚",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "${counts.studyable}",
                style = MaterialTheme.typography.headlineSmall,
                color = if (counts.studyable > 0) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(10.dp))
        CountRow(counts.newCount, counts.learnCount, counts.dueCount)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextActions("カードを見る", onBrowse)
            TextActions("デッキ設定", onEdit)
        }
    }
}

@Composable
private fun TextActions(label: String, onClick: () -> Unit) {
    Text(
        label,
        modifier = Modifier.clickable(onClick = onClick).padding(vertical = 4.dp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}
