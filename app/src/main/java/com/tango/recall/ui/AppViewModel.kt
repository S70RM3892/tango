package com.tango.recall.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tango.recall.data.Card
import com.tango.recall.data.Deck
import com.tango.recall.data.DeckCounts
import com.tango.recall.data.AnswerMode
import com.tango.recall.data.ConfusionPair
import com.tango.recall.data.ExamOutlook
import com.tango.recall.data.Grade
import com.tango.recall.data.GradeResult
import com.tango.recall.data.GraphData
import com.tango.recall.data.ImportExport
import com.tango.recall.data.ImportResult
import com.tango.recall.data.LinkType
import com.tango.recall.data.Note
import com.tango.recall.data.NoteType
import com.tango.recall.data.RelatedNote
import com.tango.recall.data.RenderedCard
import com.tango.recall.data.Repository
import com.tango.recall.data.Seed
import com.tango.recall.data.Stats
import com.tango.recall.data.TangoDb
import com.tango.recall.data.gradeNumeric
import com.tango.recall.data.gradeSelfCheck
import com.tango.recall.data.gradeTyped
import com.tango.recall.srs.Rating
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A mix-up caught in the act: the note whose answer was written instead of the
 * right one.
 */
data class ConfusionHit(val other: Note, val times: Int, val autoLinked: Boolean)

/** Live state of one study session. */
data class ReviewSession(
    val deckId: Long?,
    val deckName: String,
    val queue: List<Long> = emptyList(),
    val position: Int = 0,
    val current: RenderedCard? = null,
    val related: List<RelatedNote> = emptyList(),
    val revealed: Boolean = false,
    val typed: String = "",
    val grade: GradeResult? = null,
    /** For self-graded cards: which checklist points the learner ticked. */
    val checked: Set<Int> = emptySet(),
    val confusion: ConfusionHit? = null,
    val examMode: Boolean = false,
    val previews: Map<Rating, Long> = emptyMap(),
    val answered: Int = 0,
    val correct: Int = 0,
    val shownAt: Long = System.currentTimeMillis(),
) {
    val remaining: Int get() = (queue.size - position).coerceAtLeast(0)
    val finished: Boolean get() = current == null
}

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repository(TangoDb(app))

    var decks by mutableStateOf<List<Deck>>(emptyList()); private set
    var counts by mutableStateOf<Map<Long, DeckCounts>>(emptyMap()); private set
    var session by mutableStateOf<ReviewSession?>(null); private set
    var stats by mutableStateOf<Stats?>(null); private set
    var toast by mutableStateOf<String?>(null)
    var busy by mutableStateOf(false); private set

    init {
        viewModelScope.launch {
            io { Seed.populate(repo) }
            refresh()
        }
    }

    private suspend fun <T> io(block: () -> T): T = withContext(Dispatchers.IO) { block() }

    fun refresh() = viewModelScope.launch {
        val d = io { repo.listDecks() }
        val c = io { repo.deckCounts() }
        decks = d
        counts = c
    }

    fun loadStats() = viewModelScope.launch { stats = io { repo.stats() } }

    // ---- settings -----------------------------------------------------------

    val desiredRetention: Double get() = repo.desiredRetention
    val showRelated: Boolean get() = repo.showRelated
    val maxReviewsPerDay: Int get() = repo.maxReviewsPerDay

    fun setDesiredRetention(v: Double) = viewModelScope.launch {
        io { repo.desiredRetention = v }; refresh()
    }

    fun setShowRelated(v: Boolean) = viewModelScope.launch { io { repo.showRelated = v }; refresh() }

    fun setMaxReviews(v: Int) = viewModelScope.launch { io { repo.maxReviewsPerDay = v }; refresh() }

    // ---- decks --------------------------------------------------------------

    suspend fun deck(id: Long): Deck? = io { repo.deck(id) }

    fun saveDeck(deck: Deck, onDone: (Long) -> Unit = {}) = viewModelScope.launch {
        val id = io { repo.saveDeck(deck) }
        refresh()
        onDone(id)
    }

    fun deleteDeck(id: Long) = viewModelScope.launch { io { repo.deleteDeck(id) }; refresh() }

    // ---- notes --------------------------------------------------------------

    suspend fun note(id: Long): Note? = io { repo.note(id) }

    suspend fun notes(deckId: Long?, query: String): List<Note> = io { repo.listNotes(deckId, query) }

    suspend fun cardsOfNote(id: Long): List<Card> = io { repo.cardsOfNote(id) }

    fun saveNote(note: Note, onDone: (Long) -> Unit = {}) = viewModelScope.launch {
        val id = io { repo.saveNote(note) }
        refresh()
        onDone(id)
    }

    fun deleteNote(id: Long, onDone: () -> Unit = {}) = viewModelScope.launch {
        io { repo.deleteNote(id) }; refresh(); onDone()
    }

    // ---- relations ----------------------------------------------------------

    suspend fun related(noteId: Long): List<RelatedNote> = io { repo.related(noteId) }

    suspend fun suggestions(note: Note): List<Pair<Note, LinkType>> = io { repo.suggestLinks(note) }

    fun addLink(from: Long, to: Long, type: LinkType, memo: String = "", onDone: () -> Unit = {}) =
        viewModelScope.launch {
            val ok = io { repo.addLink(from, to, type, memo) }
            if (!ok) toast = "その関係はすでに登録されています"
            onDone()
        }

    fun deleteLink(id: Long, onDone: () -> Unit = {}) =
        viewModelScope.launch { io { repo.deleteLink(id) }; onDone() }

    // ---- review -------------------------------------------------------------

    fun startReview(deckId: Long?, exam: Boolean = false) = viewModelScope.launch {
        busy = true
        val name = when {
            exam -> "試験日から逆算"
            deckId != null -> io { repo.deck(deckId) }?.name ?: "デッキ"
            else -> "すべてのデッキ"
        }
        val queue = if (exam) io { repo.buildExamQueue() } else io { repo.buildQueue(deckId) }
        session = ReviewSession(deckId = deckId, deckName = name, queue = queue, examMode = exam)
        advance(0)
        busy = false
    }

    fun endReview() {
        session = null
        refresh()
    }

    private suspend fun advance(position: Int) {
        val s = session ?: return
        var p = position
        while (p < s.queue.size) {
            val cardId = s.queue[p]
            val card = io { repo.card(cardId) }
            val note = card?.let { io { repo.note(it.noteId) } }
            val rendered = if (card != null && note != null) io { repo.renderAnyCard(card, note) } else null
            if (rendered != null) {
                val rel = if (repo.showRelated) io { repo.related(note!!.id) } else emptyList()
                val previews = io { repo.scheduler().previewDelays(card!!.srs, System.currentTimeMillis()) }
                session = s.copy(
                    position = p,
                    current = rendered,
                    related = rel,
                    revealed = false,
                    typed = "",
                    grade = null,
                    checked = emptySet(),
                    confusion = null,
                    previews = previews,
                    shownAt = System.currentTimeMillis(),
                )
                return
            }
            p++
        }
        session = s.copy(position = s.queue.size, current = null, related = emptyList(), revealed = false)
    }

    fun updateTyped(value: String) {
        session = session?.copy(typed = value)
    }

    /** Reveal the answer; for typed and numeric cards this also grades what was entered. */
    fun reveal() {
        val s = session ?: return
        val card = s.current ?: return
        val grade = when (card.mode) {
            AnswerMode.REVEAL -> null
            AnswerMode.NUMERIC -> gradeNumeric(s.typed, card.expectedAnswer, card.tolerancePercent, card.unit)
            // Nothing is ticked yet, so this starts at the bottom and rises as the
            // learner works down the checklist.
            AnswerMode.SELF_CHECK -> gradeSelfCheck(0, card.checklist.size)
            AnswerMode.TYPE, AnswerMode.CLOZE -> {
                val chemistry = card.note.type == NoteType.CHEM_SUBSTANCE ||
                    card.note.type == NoteType.CHEM_REACTION
                gradeTyped(s.typed, card.expectedAnswer, chemistry)
            }
        }
        session = s.copy(revealed = true, grade = grade)

        val typedModes = card.mode == AnswerMode.TYPE || card.mode == AnswerMode.CLOZE ||
            card.mode == AnswerMode.NUMERIC
        if (typedModes && grade?.grade == Grade.WRONG) {
            viewModelScope.launch { detectConfusion(card, s.typed) }
        }
    }

    /**
     * Work out whether the wrong answer was actually another note's answer, and let
     * the relation graph grow out of it.
     *
     * A single slip only gets offered as a suggestion; a pair mixed up repeatedly is
     * linked automatically, because by then it is a fact about this learner's memory
     * rather than a typo.
     */
    private suspend fun detectConfusion(card: RenderedCard, typed: String) {
        val other = io { repo.findConfusion(card.note, card.template.id, typed) } ?: return
        val times = io { repo.recordConfusion(card.note.id, other.id, card.template.id, typed) }
        val alreadyLinked = io { repo.areLinked(card.note.id, other.id) }
        var autoLinked = false
        if (!alreadyLinked && times >= CONFUSION_LINK_THRESHOLD) {
            autoLinked = io {
                repo.addLink(card.note.id, other.id, LinkType.CONFUSABLE, "取り違え ${times} 回")
            }
        }
        val current = session ?: return
        if (current.current?.card?.id != card.card.id) return
        session = current.copy(
            confusion = ConfusionHit(other, times, autoLinked || alreadyLinked),
        )
    }

    /** Add the suggested "混同注意" relation by hand. */
    fun linkConfusion() = viewModelScope.launch {
        val s = session ?: return@launch
        val hit = s.confusion ?: return@launch
        val noteId = s.current?.note?.id ?: return@launch
        io { repo.addLink(noteId, hit.other.id, LinkType.CONFUSABLE, "取り違え ${hit.times} 回") }
        session = session?.copy(confusion = hit.copy(autoLinked = true))
        toast = "「混同注意」でつなぎました"
    }

    /** Tick or untick one point on a self-graded card. */
    fun toggleCheck(index: Int) {
        val s = session ?: return
        val card = s.current ?: return
        val checked = if (index in s.checked) s.checked - index else s.checked + index
        session = s.copy(checked = checked, grade = gradeSelfCheck(checked.size, card.checklist.size))
    }

    fun rate(rating: Rating) = viewModelScope.launch {
        val s = session ?: return@launch
        val rendered = s.current ?: return@launch
        val now = System.currentTimeMillis()
        val took = (now - s.shownAt).coerceAtMost(10 * 60_000L)
        val updated = io { repo.answer(rendered.card, rating, now, took) }

        // A card that is coming back within the session gets requeued a few places
        // later instead of being lost until tomorrow.
        var queue = s.queue
        val dueInMs = updated.srs.due - now
        if (dueInMs in 0..REQUEUE_HORIZON_MS) {
            val insertAt = (s.position + REQUEUE_GAP).coerceAtMost(queue.size)
            queue = queue.toMutableList().also { it.add(insertAt, updated.id) }
        }

        session = s.copy(
            queue = queue,
            answered = s.answered + 1,
            correct = s.correct + if (rating != Rating.AGAIN) 1 else 0,
        )
        advance(s.position + 1)
    }

    /** Skip without grading. */
    fun skip() = viewModelScope.launch {
        val s = session ?: return@launch
        advance(s.position + 1)
    }

    fun suspendCurrent() = viewModelScope.launch {
        val s = session ?: return@launch
        val card = s.current?.card ?: return@launch
        io { repo.setSuspended(card.id, true) }
        toast = "このカードを保留しました"
        advance(s.position + 1)
    }

    // ---- import / export ----------------------------------------------------

    fun exportBackup(uri: Uri) = viewModelScope.launch {
        busy = true
        toast = try {
            io {
                val json = ImportExport.exportJson(repo)
                getApplication<Application>().contentResolver.openOutputStream(uri)?.use {
                    it.write(json.toByteArray())
                }
            }
            "バックアップを書き出しました"
        } catch (e: Exception) {
            "書き出しに失敗しました: ${e.message}"
        }
        busy = false
    }

    fun importBackup(uri: Uri) = viewModelScope.launch {
        busy = true
        val result = try {
            io {
                val text = getApplication<Application>().contentResolver.openInputStream(uri)
                    ?.bufferedReader()?.use { it.readText() } ?: ""
                ImportExport.importJson(repo, text)
            }
        } catch (e: Exception) {
            ImportResult(0, 0, 0, "読み込みに失敗しました: ${e.message}")
        }
        toast = result.message
        busy = false
        refresh()
    }

    fun importDelimited(deckId: Long, uri: Uri) = viewModelScope.launch {
        busy = true
        val result = try {
            io {
                val text = getApplication<Application>().contentResolver.openInputStream(uri)
                    ?.bufferedReader()?.use { it.readText() } ?: ""
                ImportExport.importDelimited(repo, deckId, text)
            }
        } catch (e: Exception) {
            ImportResult(0, 0, 0, "読み込みに失敗しました: ${e.message}")
        }
        toast = result.message
        busy = false
        refresh()
    }

    fun importDelimitedText(deckId: Long, text: String) = viewModelScope.launch {
        busy = true
        val result = io { ImportExport.importDelimited(repo, deckId, text) }
        toast = result.message
        busy = false
        refresh()
    }

    fun exportDeckTsv(deckId: Long, uri: Uri) = viewModelScope.launch {
        busy = true
        toast = try {
            io {
                val tsv = ImportExport.exportTsv(repo, deckId)
                getApplication<Application>().contentResolver.openOutputStream(uri)?.use {
                    it.write(tsv.toByteArray())
                }
            }
            "TSV を書き出しました"
        } catch (e: Exception) {
            "書き出しに失敗しました: ${e.message}"
        }
        busy = false
    }

    suspend fun weakest(): List<Pair<Note, Double>> = io { repo.weakest() }

    suspend fun graph(deckId: Long?): GraphData = io { repo.graph(deckId) }

    // ---- exam countdown -----------------------------------------------------

    var examOutlook by mutableStateOf<ExamOutlook?>(null); private set

    val examDate: Long get() = repo.examDate

    fun loadExamOutlook() = viewModelScope.launch { examOutlook = io { repo.examOutlook() } }

    fun setExamDate(value: Long) = viewModelScope.launch {
        io { repo.examDate = value }
        examOutlook = io { repo.examOutlook() }
        refresh()
    }

    suspend fun confusionPairs(): List<ConfusionPair> = io { repo.confusionPairs() }

    companion object {
        private const val REQUEUE_HORIZON_MS = 20 * 60_000L
        private const val REQUEUE_GAP = 8

        /** Mix-ups needed before a pair is linked without being asked. */
        private const val CONFUSION_LINK_THRESHOLD = 2
    }
}
