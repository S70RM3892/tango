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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
import com.tango.recall.data.NoteType
import com.tango.recall.ui.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckScreen(vm: AppViewModel, nav: NavController, deckId: Long) {
    val isNew = deckId == 0L
    var name by remember { mutableStateOf("") }
    var noteType by remember { mutableStateOf(NoteType.ENGLISH) }
    var enabled by remember { mutableStateOf<Set<String>>(emptySet()) }
    var newPerDay by remember { mutableStateOf(15f) }
    var created by remember { mutableStateOf(System.currentTimeMillis()) }
    var typeMenu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var pasteImport by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(isNew) }

    LaunchedEffect(deckId) {
        if (!isNew) {
            vm.deck(deckId)?.let { d ->
                name = d.name
                noteType = d.noteType
                enabled = d.enabledTemplates
                newPerDay = d.newPerDay.toFloat()
                created = d.created
            }
        } else {
            enabled = NoteType.ENGLISH.templates.filter { it.defaultEnabled }.map { it.id }.toSet()
        }
        loaded = true
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null && !isNew) vm.importDelimited(deckId, uri) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/tab-separated-values")
    ) { uri -> if (uri != null && !isNew) vm.exportDeckTsv(deckId, uri) }

    Scaffold(
        topBar = {
            TangoTopBar(
                if (isNew) "新しいデッキ" else "デッキ設定",
                onBack = { nav.popBackStack() },
            ) {
                if (!isNew) {
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "削除")
                    }
                }
            }
        },
    ) { padding ->
        if (!loaded) return@Scaffold
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("デッキ名") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            SectionCard {
                SectionTitle("ノートの型")
                Spacer(Modifier.height(8.dp))
                Column {
                    OutlinedButton(
                        onClick = { if (isNew) typeMenu = true },
                        enabled = isNew,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(noteType.label) }
                    DropdownMenu(expanded = typeMenu, onDismissRequest = { typeMenu = false }) {
                        NoteType.entries.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t.label) },
                                onClick = {
                                    noteType = t
                                    enabled = t.templates.filter { it.defaultEnabled }.map { it.id }.toSet()
                                    typeMenu = false
                                },
                            )
                        }
                    }
                }
                if (!isNew) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "作成後は型を変えられません（欄の構成が変わるため）。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            SectionCard {
                SectionTitle("出題する向き")
                Spacer(Modifier.height(4.dp))
                Text(
                    "1つのノートから、選んだ向きのぶんだけカードが作られます。逆向きは別の記憶なので、別々に管理されます。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                noteType.templates.forEach { tpl ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = tpl.id in enabled,
                            onCheckedChange = { on ->
                                enabled = if (on) enabled + tpl.id else enabled - tpl.id
                            },
                        )
                        Column {
                            Text(tpl.label, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "必要な欄: " + tpl.requires.joinToString("、") { noteType.label(it) },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            SectionCard {
                SectionTitle("1日の新規カード上限：${newPerDay.toInt()} 枚")
                Slider(
                    value = newPerDay,
                    onValueChange = { newPerDay = it },
                    valueRange = 0f..60f,
                    steps = 59,
                )
                Text(
                    "新しく覚える量を絞ると、翌日以降の復習が膨らみません。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (!isNew) {
                SectionCard {
                    SectionTitle("この デッキの読み込み・書き出し")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "表計算ソフトで作った TSV / CSV をそのまま取り込めます。1行目が欄の名前なら見出しとして扱います。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("text/*", "text/csv", "text/tab-separated-values")) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("ファイルから取り込む") }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { pasteImport = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("貼り付けて取り込む") }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { exportLauncher.launch("${name.ifBlank { "deck" }}.tsv") },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("TSV で書き出す") }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "列の順番: " + noteType.fields.joinToString("、") { it.label } + "、タグ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Button(
                onClick = {
                    vm.saveDeck(
                        Deck(
                            id = deckId,
                            name = name.ifBlank { "名前のないデッキ" },
                            noteTypeId = noteType.id,
                            enabledTemplates = enabled,
                            newPerDay = newPerDay.toInt(),
                            created = created,
                        )
                    ) { nav.popBackStack() }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = enabled.isNotEmpty(),
            ) { Text("保存") }

            if (enabled.isEmpty()) {
                Text(
                    "少なくとも1つの向きを選んでください。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("このデッキを削除しますか") },
            text = { Text("含まれるノートとカードもすべて削除されます。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    vm.deleteDeck(deckId)
                    nav.popBackStack()
                }) { Text("削除") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("やめる") } },
        )
    }

    if (pasteImport && !isNew) {
        PasteImportDialog(
            fieldOrder = noteType.fields.joinToString("\t") { it.label },
            onDismiss = { pasteImport = false },
            onImport = { text -> pasteImport = false; vm.importDelimitedText(deckId, text) },
        )
    }
}

@Composable
private fun PasteImportDialog(fieldOrder: String, onDismiss: () -> Unit, onImport: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("貼り付けて取り込む") },
        text = {
            Column {
                Text(
                    "1行に1ノート。タブまたはカンマ区切りで、次の順に並べます:\n$fieldOrder",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    minLines = 6,
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onImport(text) }, enabled = text.isNotBlank()) { Text("取り込む") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("やめる") } },
    )
}
