package com.tango.recall.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.tango.recall.Routes
import com.tango.recall.data.RootShelf
import com.tango.recall.data.WordCell
import com.tango.recall.ui.AppViewModel
import kotlin.math.roundToInt

/**
 * The word shelf.
 *
 * Vocabulary does not lay out as a map. Seventy roots are seventy little cliques with
 * almost nothing joining them, and a force layout of disconnected cliques gives a
 * field of blobs: no shape to learn, no place to remember, and three hundred labels
 * fighting for the same pixels.
 *
 * What a vocabulary actually varies along is one dimension — how well each word is
 * held — so this lays it out along that one: a row per root, weakest root at the top,
 * each word a chip whose brightness is its predicted recall. The dim rows are where
 * the time should go, and the row itself is a study button, because the root is the
 * unit the words were learned in.
 */
@Composable
fun WordShelfScreen(vm: AppViewModel, nav: NavController) {
    var shelves by remember { mutableStateOf<List<RootShelf>?>(null) }
    var horizonDays by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) { shelves = vm.wordShelf() }

    // Pinned once per visit: reading the clock during composition would make every
    // recomposition a slightly different "now".
    val openedAt = remember { System.currentTimeMillis() }
    val examDays = remember(vm.examDate, openedAt) {
        vm.examDate.takeIf { it > 0 }
            ?.let { ((it - openedAt) / 86_400_000L).toInt() }
            ?.takeIf { it > 0 }
    }
    val at = openedAt + horizonDays * 86_400_000L

    Scaffold(topBar = { TangoTopBar("英単語の棚", onBack = { nav.popBackStack() }) }) { padding ->
        val shelf = shelves
        when {
            shelf == null -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator()
            }

            shelf.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Text("英単語のノートがまだありません。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            else -> LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    ShelfSummary(
                        shelves = shelf,
                        at = at,
                        horizonDays = horizonDays,
                        examDays = examDays,
                        onHorizon = { horizonDays = it },
                    )
                }
                items(shelf, key = { it.root }) { row ->
                    RootRow(
                        shelf = row,
                        at = at,
                        onWord = { word -> nav.navigate(Routes.note(word.noteId, word.deckId)) },
                        onStudy = {
                            vm.startGroupReview(row.root, row.words.map { it.noteId }) {
                                nav.navigate(Routes.SESSION)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ShelfSummary(
    shelves: List<RootShelf>,
    at: Long,
    horizonDays: Int,
    examDays: Int?,
    onHorizon: (Int) -> Unit,
) {
    val words = shelves.sumOf { it.words.size }
    val studied = shelves.sumOf { it.studied.size }
    val mean = shelves.flatMap { it.studied }.takeIf { it.isNotEmpty() }
        ?.map { it.strengthAt(at) }?.average()
    val fading = shelves.flatMap { it.studied }.count { it.strengthAt(at) < 0.8 }

    SectionCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            StatTile("$words", "語")
            StatTile("$studied", "学習済み")
            StatTile(mean?.let { "${(it * 100).roundToInt()}%" } ?: "—", "平均")
            StatTile("$fading", "弱い語")
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "明るいチップほど思い出しやすい語です。暗い語根から順に並んでいるので、" +
                "上から順に手を入れれば、いちばん崩れているところから直せます。",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val stops = buildList {
                add(0 to "いま")
                add(7 to "1週間後")
                add(30 to "1か月後")
                if (examDays != null) add(examDays to "試験日")
            }
            stops.forEach { (days, label) ->
                FilterChip(
                    selected = horizonDays == days,
                    onClick = { onHorizon(days) },
                    label = { Text(label) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RootRow(
    shelf: RootShelf,
    at: Long,
    onWord: (WordCell) -> Unit,
    onStudy: () -> Unit,
) {
    val mean = shelf.meanAt(at)
    SectionCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    shelf.root,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    buildString {
                        append("${shelf.words.size} 語")
                        if (shelf.newCount > 0) append(" ・ 未学習 ${shelf.newCount}")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Pill(mean?.let { "${(it * 100).roundToInt()}%" } ?: "未学習")
        }
        Spacer(Modifier.height(10.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            shelf.words.forEach { word -> WordChip(word, at, onWord) }
        }
        Spacer(Modifier.height(6.dp))
        TextButton(onClick = onStudy) { Text("この語根をまとめて学習") }
    }
}

/**
 * One word, lit by how well it is held.
 *
 * Brightness rather than a number: a row of percentages is unreadable at a glance,
 * while a row of tiles is a picture of which corner of the vocabulary is going dark.
 */
@Composable
private fun WordChip(word: WordCell, at: Long, onClick: (WordCell) -> Unit) {
    val strength = word.strengthAt(at).toFloat()
    val scheme = MaterialTheme.colorScheme
    val container = when {
        word.isNew -> scheme.surface
        else -> scheme.primary.copy(alpha = 0.10f + 0.45f * strength)
    }
    Surface(
        color = container,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (word.isNew) {
                    Modifier.border(1.dp, scheme.outline, RoundedCornerShape(8.dp))
                } else {
                    Modifier
                }
            )
            .clickable { onClick(word) },
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text(word.word, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            if (word.meaning.isNotBlank()) {
                Text(
                    word.meaning,
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
