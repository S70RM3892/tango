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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.tango.recall.Routes
import com.tango.recall.data.AnswerMode
import com.tango.recall.data.Grade
import com.tango.recall.data.Note
import com.tango.recall.data.RelatedNote
import com.tango.recall.data.RenderedCard
import com.tango.recall.data.humanDelay
import com.tango.recall.srs.Rating
import com.tango.recall.ui.AppViewModel
import com.tango.recall.ui.ConfusionHit
import com.tango.recall.ui.ReviewSession

@Composable
fun ReviewScreen(vm: AppViewModel, nav: NavController, deckId: Long?, exam: Boolean = false) {
    LaunchedEffect(deckId, exam) { vm.startReview(deckId, exam) }
    val session = vm.session
    var peek by remember { mutableStateOf<Note?>(null) }

    Scaffold(
        topBar = {
            TangoTopBar(
                title = session?.deckName ?: "学習",
                onBack = { vm.endReview(); nav.popBackStack() },
            ) {
                if (session?.current != null) {
                    IconButton(onClick = {
                        val c = session.current
                        vm.endReview()
                        nav.navigate(Routes.note(c.note.id, c.note.deckId))
                    }) { Icon(Icons.Default.Edit, contentDescription = "編集") }
                    IconButton(onClick = { vm.suspendCurrent() }) {
                        Icon(AppIcons.PauseCircle, contentDescription = "保留")
                    }
                    IconButton(onClick = { vm.skip() }) {
                        Icon(AppIcons.SkipNext, contentDescription = "スキップ")
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                session == null || vm.busy -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator()
                }

                session.finished -> SessionSummary(session, vm, nav, deckId)

                else -> CardPane(session, vm) { peek = it }
            }
        }
    }

    peek?.let { note -> RelatedSheet(note, vm, nav) { peek = null } }
}

@Composable
private fun CardPane(session: ReviewSession, vm: AppViewModel, onPeek: (Note) -> Unit) {
    val card = session.current ?: return
    val progress = if (session.queue.isEmpty()) 0f
    else session.position.toFloat() / session.queue.size

    Column(Modifier.fillMaxSize().imePadding()) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(3.dp),
        )

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Pill(card.template.label)
                Text(
                    "残り ${session.remaining}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(20.dp))
            QuestionBlock(card)

            if (card.mode != AnswerMode.REVEAL && !session.revealed) {
                Spacer(Modifier.height(20.dp))
                TypeAnswerField(card, session, vm)
            }

            if (session.revealed) {
                Spacer(Modifier.height(20.dp))
                session.grade?.let { GradeBanner(it.grade, it.comment) }
                session.confusion?.let {
                    Spacer(Modifier.height(12.dp))
                    ConfusionBlock(it, vm)
                }
                Spacer(Modifier.height(12.dp))
                AnswerBlock(card, typed = session.typed)

                if (card.mode == AnswerMode.SELF_CHECK && card.checklist.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    ChecklistBlock(card, session, vm)
                }

                if (session.related.isNotEmpty() && !card.isRelation) {
                    Spacer(Modifier.height(20.dp))
                    RelatedBlock(session.related, onPeek)
                }
            }

            Spacer(Modifier.height(24.dp))
        }

        HorizontalDivider()
        AnswerBar(session, vm)
    }
}

@Composable
private fun QuestionBlock(card: RenderedCard) {
    Text(
        card.promptLabel,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(6.dp))
    Text(
        card.promptText,
        style = if (card.promptText.length <= 24) MaterialTheme.typography.displaySmall
        else MaterialTheme.typography.headlineSmall,
        fontFamily = if (card.mode == AnswerMode.CLOZE) FontFamily.Default else null,
    )
    if (card.promptExtras.isNotEmpty()) {
        Spacer(Modifier.height(12.dp))
        card.promptExtras.forEach { (label, value) ->
            Text(
                "$label: $value",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TypeAnswerField(card: RenderedCard, session: ReviewSession, vm: AppViewModel) {
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(card.card.id) { runCatching { focus.requestFocus() } }

    val label = when (card.mode) {
        AnswerMode.SELF_CHECK -> "英語で書いてみる（提出してから自分で採点します）"
        AnswerMode.NUMERIC -> if (card.unit.isBlank()) "答えの数値" else "答えの数値（単位は ${card.unit}）"
        else -> "答えを入力（書けると記憶は強くなります）"
    }
    // A plain decimal pad is nicer, but an answer written in scientific notation
    // needs the full keyboard to type the exponent.
    val needsExponent = card.expectedAnswer.contains(Regex("[eE^×x]"))
    val keyboardType = if (card.mode == AnswerMode.NUMERIC && !needsExponent) {
        KeyboardType.Decimal
    } else KeyboardType.Text
    val multiline = card.mode == AnswerMode.SELF_CHECK

    OutlinedTextField(
        value = session.typed,
        onValueChange = vm::updateTyped,
        modifier = Modifier.fillMaxWidth().focusRequester(focus),
        label = { Text(label) },
        placeholder = {
            if (card.mode == AnswerMode.NUMERIC && needsExponent) Text("例: 2.7e-5 / 2.7×10^-5")
        },
        suffix = { if (card.mode == AnswerMode.NUMERIC && card.unit.isNotBlank()) Text(card.unit) },
        singleLine = false,
        minLines = if (multiline) 3 else 1,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = if (multiline) ImeAction.Default else ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { keyboard?.hide(); vm.reveal() }),
    )
}

/**
 * The self-grading checklist.
 *
 * Translation into English cannot be marked by string comparison, so the learner
 * checks off the points the model answer required. Ticking a box updates the
 * suggested button immediately.
 */
@Composable
private fun ChecklistBlock(card: RenderedCard, session: ReviewSession, vm: AppViewModel) {
    SectionCard {
        SectionTitle("押さえるべき点")
        Spacer(Modifier.height(4.dp))
        Text(
            "書いた英文と模範解答を見比べて、できていた項目にチェックしてください。",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        card.checklist.forEachIndexed { index, item ->
            Row(
                Modifier.fillMaxWidth().clickable { vm.toggleCheck(index) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = index in session.checked, onCheckedChange = { vm.toggleCheck(index) })
                Text(item, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun GradeBanner(grade: Grade, comment: String) {
    val (container, content, title) = when (grade) {
        Grade.CORRECT -> Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer, "正解",
        )
        Grade.CLOSE -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer, "惜しい",
        )
        Grade.WRONG -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer, "不正解",
        )
    }
    Surface(
        color = container,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = content)
            Text(comment, style = MaterialTheme.typography.bodyMedium, color = content)
        }
    }
}

/**
 * What the wrong answer actually was.
 *
 * Writing one note's answer where another belonged is a specific, repeatable fact
 * about this learner's memory, so it is offered as a relation rather than thrown
 * away as a generic "wrong".
 */
@Composable
private fun ConfusionBlock(hit: ConfusionHit, vm: AppViewModel) {
    SectionCard {
        SectionTitle("取り違えたのはこれですね")
        Spacer(Modifier.height(8.dp))
        Text(hit.other.title(), style = MaterialTheme.typography.titleMedium)
        val sub = hit.other.subtitle()
        if (sub.isNotBlank()) {
            Text(
                sub,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(10.dp))
        if (hit.autoLinked) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Pill("混同注意でつながりました")
                Text(
                    "  ${hit.times} 回目",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "次からは、どちらを復習しても相手が一緒に出てきます。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            OutlinedButton(onClick = { vm.linkConfusion() }, modifier = Modifier.fillMaxWidth()) {
                Text("「混同注意」でつなぐ")
            }
        }
    }
}

@Composable
private fun AnswerBlock(card: RenderedCard, typed: String) {
    SectionCard {
        card.answerParts.forEachIndexed { i, (label, value) ->
            if (i > 0) Spacer(Modifier.height(12.dp))
            LabeledValue(label, value)
        }
        if (typed.isNotBlank() && card.mode != AnswerMode.REVEAL) {
            Spacer(Modifier.height(12.dp))
            LabeledValue("あなたの答え", typed)
        }
        val memo = card.note["memo"]
        if (memo.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            LabeledValue("メモ", memo)
        }
    }
}

/**
 * The neighbours of the note just answered.
 *
 * Shown immediately after the reveal, while the item is still active in memory —
 * that is when a new connection is cheapest to form.
 */
@Composable
private fun RelatedBlock(related: List<RelatedNote>, onPeek: (Note) -> Unit) {
    SectionTitle("つながり")
    Spacer(Modifier.height(8.dp))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        related.take(8).forEach { rel ->
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                onClick = { onPeek(rel.other) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Pill(rel.label)
                        Spacer(Modifier.height(0.dp))
                        Text(
                            "  ${rel.other.title()}",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    val sub = rel.other.subtitle()
                    if (sub.isNotBlank()) {
                        Text(
                            sub,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (rel.link.memo.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            rel.link.memo,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnswerBar(session: ReviewSession, vm: AppViewModel) {
    val card = session.current ?: return
    Column(Modifier.padding(16.dp)) {
        if (!session.revealed) {
            Button(
                onClick = { vm.reveal() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text(
                    when (card.mode) {
                        AnswerMode.REVEAL -> "答えを見る"
                        AnswerMode.SELF_CHECK -> "模範解答と見比べる"
                        else -> "答え合わせ"
                    }
                )
            }
        } else {
            val suggested = session.grade?.suggestedRating
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Rating.entries.forEach { rating ->
                    val delay = session.previews[rating]
                    val isSuggested = rating == suggested
                    val colors = when (rating) {
                        Rating.AGAIN -> ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        Rating.HARD -> ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                        Rating.GOOD -> ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Rating.EASY -> ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                    Button(
                        onClick = { vm.rate(rating) },
                        modifier = Modifier.weight(1f).height(60.dp),
                        colors = colors,
                        contentPadding = PaddingValues(2.dp),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                if (isSuggested) "▸ ${rating.labelJa}" else rating.labelJa,
                                style = MaterialTheme.typography.labelLarge,
                                textAlign = TextAlign.Center,
                            )
                            if (delay != null) {
                                Text(
                                    humanDelay(delay),
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionSummary(session: ReviewSession, vm: AppViewModel, nav: NavController, deckId: Long?) {
    val accuracy = if (session.answered == 0) 0
    else (session.correct * 100 / session.answered)
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("今回の学習は終わりです", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            StatTile("${session.answered}", "解答数")
            StatTile("$accuracy%", "正答率")
        }
        Spacer(Modifier.height(32.dp))
        Button(
            // Stay in the mode the session was started in: in exam mode a fresh queue
            // must again be "weakest on the day", not today's due cards.
            onClick = { vm.startReview(deckId, session.examMode) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("もう一度キューを作る") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { vm.endReview(); nav.popBackStack() },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("ホームに戻る") }
    }
}

/** Peek at a linked note without leaving the session. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RelatedSheet(note: Note, vm: AppViewModel, nav: NavController, onDismiss: () -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var related by remember(note.id) { mutableStateOf<List<RelatedNote>>(emptyList()) }
    LaunchedEffect(note.id) { related = vm.related(note.id) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state) {
        LazyColumn(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            item {
                Text(note.title(), style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                note.type.fields.forEach { f ->
                    val v = note[f.id]
                    if (v.isNotBlank() && f.id != note.type.fields.first().id) {
                        Spacer(Modifier.height(8.dp))
                        LabeledValue(f.label, v)
                    }
                }
                if (related.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    SectionTitle("この語のつながり")
                    Spacer(Modifier.height(8.dp))
                }
            }
            items(related) { rel ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Pill(rel.label, Color.Transparent, MaterialTheme.colorScheme.primary)
                    Text("  ${rel.other.title()}", style = MaterialTheme.typography.bodyLarge)
                }
            }
            item {
                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { onDismiss(); vm.endReview(); nav.navigate(Routes.note(note.id, note.deckId)) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("このノートを編集") }
            }
        }
    }
}
