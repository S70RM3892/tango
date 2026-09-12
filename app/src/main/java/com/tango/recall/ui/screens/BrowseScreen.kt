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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.tango.recall.Routes
import com.tango.recall.data.Note
import com.tango.recall.ui.AppViewModel
import kotlinx.coroutines.delay

@Composable
fun BrowseScreen(vm: AppViewModel, nav: NavController, deckId: Long?) {
    var query by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf<List<Note>>(emptyList()) }
    var pickDeck by remember { mutableStateOf(false) }
    val deckName = vm.decks.firstOrNull { it.id == deckId }?.name ?: "すべてのカード"

    LaunchedEffect(deckId, query) {
        // Every keystroke scans the collection, and most keystrokes are on the way to a
        // word rather than at it; wait until the typing pauses.
        if (query.isNotBlank()) delay(SEARCH_DEBOUNCE_MS)
        notes = vm.notes(deckId, query)
    }

    Scaffold(
        topBar = { TangoTopBar(deckName, onBack = { nav.popBackStack() }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (deckId != null) nav.navigate(Routes.note(0L, deckId))
                    else pickDeck = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("ノートを追加") },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("語・意味・化学式・タグで検索") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )

            if (notes.isEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text(
                        if (query.isBlank()) "まだノートがありません。" else "見つかりませんでした。",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        Text(
                            "${notes.size} 件",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    items(notes, key = { it.id }) { note ->
                        SectionCard(
                            modifier = Modifier.clickable {
                                nav.navigate(Routes.note(note.id, note.deckId))
                            }
                        ) {
                            Text(note.title(), style = MaterialTheme.typography.titleMedium)
                            val sub = note.subtitle()
                            if (sub.isNotBlank()) {
                                Text(
                                    sub,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                )
                            }
                            if (note.tags.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    note.tags.take(3).forEach { Pill(it) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (pickDeck) {
        AlertDialog(
            onDismissRequest = { pickDeck = false },
            title = { Text("どのデッキに追加しますか") },
            text = {
                Column {
                    vm.decks.forEach { d ->
                        Text(
                            d.name,
                            modifier = Modifier.fillMaxWidth()
                                .clickable { pickDeck = false; nav.navigate(Routes.note(0L, d.id)) }
                                .padding(vertical = 12.dp),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { pickDeck = false }) { Text("閉じる") } },
        )
    }
}
