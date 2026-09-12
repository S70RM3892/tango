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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.tango.recall.data.Deck
import com.tango.recall.data.LinkType
import com.tango.recall.data.Note
import com.tango.recall.data.NoteType
import com.tango.recall.data.RelatedNote
import com.tango.recall.ui.AppViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteScreen(vm: AppViewModel, nav: NavController, noteId: Long, deckId: Long) {
    var deck by remember { mutableStateOf<Deck?>(null) }
    var fields by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var tags by remember { mutableStateOf("") }
    var loaded by remember { mutableStateOf(false) }
    var savedId by remember { mutableStateOf(noteId) }
    var related by remember { mutableStateOf<List<RelatedNote>>(emptyList()) }
    var suggestions by remember { mutableStateOf<List<Pair<Note, LinkType>>>(emptyList()) }
    var confirmDelete by remember { mutableStateOf(false) }
    var linkPicker by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableStateOf(0) }

    val type: NoteType = deck?.noteType ?: NoteType.BASIC

    LaunchedEffect(noteId, deckId) {
        val existing = if (noteId != 0L) vm.note(noteId) else null
        deck = vm.deck(existing?.deckId ?: deckId)
        fields = existing?.fields ?: emptyMap()
        tags = existing?.tags?.joinToString(" ") ?: ""
        savedId = existing?.id ?: 0L
        loaded = true
    }

    // Relations are reloaded whenever the note gains an id or a link is added.
    LaunchedEffect(savedId, reloadKey) {
        if (savedId == 0L) {
            related = emptyList(); suggestions = emptyList()
        } else {
            related = vm.related(savedId)
            vm.note(savedId)?.let { suggestions = vm.suggestions(it) }
        }
    }

    fun persist(then: () -> Unit = {}) {
        val d = deck ?: return
        vm.saveNote(
            Note(
                id = savedId,
                deckId = d.id,
                typeId = d.noteTypeId,
                fields = fields,
                tags = tags.split(" ", ",").map { it.trim() }.filter { it.isNotEmpty() },
            )
        ) { id -> savedId = id; then() }
    }

    Scaffold(
        topBar = {
            TangoTopBar(
                title = if (noteId == 0L) "新しいノート" else "ノートを編集",
                onBack = { nav.popBackStack() },
            ) {
                if (savedId != 0L) {
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "削除")
                    }
                }
            }
        },
    ) { padding ->
        if (!loaded) return@Scaffold

        LazyColumn(
            Modifier.fillMaxSize().padding(padding).imePadding(),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 48.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "${deck?.name.orEmpty()} ・ ${type.label}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            items(type.fields, key = { it.id }) { f ->
                OutlinedTextField(
                    value = fields[f.id].orEmpty(),
                    onValueChange = { fields = fields + (f.id to it) },
                    label = { Text(f.label) },
                    placeholder = { if (f.hint.isNotBlank()) Text(f.hint) },
                    singleLine = !f.multiline,
                    minLines = if (f.multiline) 2 else 1,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("タグ（スペース区切り）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item { GeneratedCardsPreview(type, deck, fields) }

            item {
                Button(
                    onClick = { persist { nav.popBackStack() } },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) { Text("保存") }
            }

            item {
                SectionTitle("つながり")
                Spacer(Modifier.height(4.dp))
                Text(
                    "似た語・対になる語・反応でつながる物質を結び付けておくと、復習のたびに一緒に思い出せます。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (savedId == 0L) {
                item {
                    Text(
                        "先に保存すると関係を追加できます。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            items(related, key = { it.link.id }) { rel ->
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Pill(rel.label)
                                Text("  ${rel.other.title()}", style = MaterialTheme.typography.titleMedium)
                            }
                            if (rel.link.memo.isNotBlank()) {
                                Text(
                                    rel.link.memo,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        IconButton(onClick = {
                            vm.deleteLink(rel.link.id) { reloadKey++ }
                        }) { Icon(Icons.Default.Close, contentDescription = "関係を削除") }
                    }
                }
            }

            if (savedId != 0L) {
                item {
                    OutlinedButton(onClick = { linkPicker = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text("  関係を追加")
                    }
                }
            }

            if (suggestions.isNotEmpty()) {
                item {
                    SectionTitle("関連の候補（語根・タグ・分類が一致）")
                }
                items(suggestions, key = { it.first.id }) { (other, guess) ->
                    SectionCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(other.title(), style = MaterialTheme.typography.titleMedium)
                                Text(
                                    other.subtitle(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            AssistChip(
                                onClick = {
                                    vm.addLink(savedId, other.id, guess) { reloadKey++ }
                                },
                                label = { Text("${guess.forward}として追加") },
                            )
                        }
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("このノートを削除しますか") },
            text = { Text("カードと学習履歴の結びつきも失われます。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    vm.deleteNote(savedId) { nav.popBackStack() }
                }) { Text("削除") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("やめる") } },
        )
    }

    if (linkPicker && savedId != 0L) {
        LinkPickerDialog(
            vm = vm,
            fromNoteId = savedId,
            noteType = type,
            onDismiss = { linkPicker = false },
            onPicked = { target, linkType, memo ->
                vm.addLink(savedId, target.id, linkType, memo) {
                    linkPicker = false
                    reloadKey++
                }
            },
        )
    }
}

@Composable
private fun GeneratedCardsPreview(type: NoteType, deck: Deck?, fields: Map<String, String>) {
    val enabled = deck?.enabledTemplates ?: emptySet()
    val active = type.templates.filter { it.id in enabled && it.requires.all { r -> fields[r].orEmpty().isNotBlank() } }
    SectionCard {
        SectionTitle("このノートから作られるカード")
        Spacer(Modifier.height(8.dp))
        if (active.isEmpty()) {
            Text(
                "必要な欄がまだ空です。デッキ設定で有効にした方向の必須欄を埋めるとカードができます。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                active.forEach { Pill(it.label) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LinkPickerDialog(
    vm: AppViewModel,
    fromNoteId: Long,
    noteType: NoteType,
    onDismiss: () -> Unit,
    onPicked: (Note, LinkType, String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Note>>(emptyList()) }
    var selected by remember { mutableStateOf<Note?>(null) }
    var linkType by remember { mutableStateOf(LinkType.forNoteType(noteType).first()) }
    var memo by remember { mutableStateOf("") }
    var typeMenu by remember { mutableStateOf(false) }

    LaunchedEffect(query) {
        if (query.isNotBlank()) delay(SEARCH_DEBOUNCE_MS)
        results = vm.notes(null, query).filter { it.id != fromNoteId }.take(30)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (selected == null) "つなげる相手を選ぶ" else "関係の種類") },
        text = {
            Column(Modifier.heightIn(max = 420.dp)) {
                if (selected == null) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("検索（どのデッキからでも）") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyColumn {
                        items(results, key = { it.id }) { n ->
                            Column(
                                Modifier.fillMaxWidth()
                                    .padding(vertical = 8.dp),
                            ) {
                                Text(
                                    n.title(),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.fillMaxWidth().clickable { selected = n },
                                )
                                Text(
                                    n.subtitle(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                } else {
                    Text(selected!!.title(), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    Box {
                        OutlinedButton(onClick = { typeMenu = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(linkType.forward)
                        }
                        DropdownMenu(expanded = typeMenu, onDismissRequest = { typeMenu = false }) {
                            LinkType.forNoteType(noteType).forEach { t ->
                                DropdownMenuItem(
                                    text = { Text("${t.forward}（逆から見ると ${t.reverse}）") },
                                    onClick = { linkType = t; typeMenu = false },
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = memo,
                        onValueChange = { memo = it },
                        label = { Text("ひとことメモ（任意）") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            if (selected != null) {
                TextButton(onClick = { onPicked(selected!!, linkType, memo) }) { Text("追加") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("閉じる") } },
    )
}
