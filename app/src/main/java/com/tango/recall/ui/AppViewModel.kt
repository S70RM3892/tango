package com.tango.recall.ui

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tango.recall.data.AnswerMode
import com.tango.recall.data.Card
import com.tango.recall.data.ConfusionPair
import com.tango.recall.data.Deck
import com.tango.recall.data.DeckCounts
import com.tango.recall.data.Department
import com.tango.recall.data.ExamOutlook
import com.tango.recall.data.Grade
import com.tango.recall.data.GradeResult
import com.tango.recall.data.GraphData
import com.tango.recall.data.ImportExport
import com.tango.recall.data.ImportResult
import com.tango.recall.data.Leech
import com.tango.recall.data.LinkType
import com.tango.recall.data.Note
import com.tango.recall.data.NoteType
import com.tango.recall.data.RelatedNote
import com.tango.recall.data.RenderedCard
import com.tango.recall.data.ReadingProgress
import com.tango.recall.data.ReadingRecord
import com.tango.recall.data.Repository
import com.tango.recall.data.RootShelf
import com.tango.recall.data.Seed
import com.tango.recall.data.Stats
import com.tango.recall.data.Subject
import com.tango.recall.data.TangoDb
import com.tango.recall.data.Universities
import com.tango.recall.data.gradeNumeric
import com.tango.recall.data.gradeSelfCheck
import com.tango.recall.data.gradeTyped
import com.tango.recall.notify.Reminders
import com.tango.recall.srs.Rating
import com.tango.recall.ui.screens.JapanMap
import com.tango.recall.ui.screens.JapanMapLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A mix-up caught in the act: the note whose answer was written instead of the
 * right one.
 */
data class ConfusionHit(val other: Note, val times: Int, val autoLinked: Boolean)

/**
 * Everything needed to take back the last grade.
 *
 * FSRS has no notion of "that answer was not true", so undo restores the card's
 * whole scheduling state from the copy taken before the button was pressed, and puts
 * the session back where it was — same card, same answer on screen.
 */
private data class UndoPoint(val card: Card, val session: ReviewSession)

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
    /** Cards in this session that keep being answered wrong, and how often. */
    val stumbles: Map<Long, Int> = emptyMap(),
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

    private var undoPoint: UndoPoint? = null

    /** Whether the last grade can still be taken back. */
    var canUndo by mutableStateOf(false); private set

    init {
        viewModelScope.launch {
            io { Seed.populate(repo) }
            // Alarms are dropped on reboot and on app update; putting them back at
            // launch covers the update case without waiting for the next boot.
            io { Reminders.applyFromSettings(getApplication(), repo) }
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

    // ---- the daily reminder --------------------------------------------------

    val reminderEnabled: Boolean get() = repo.reminderEnabled
    val reminderHour: Int get() = repo.reminderHour
    val reminderMinute: Int get() = repo.reminderMinute

    fun setReminder(enabled: Boolean, hour: Int = repo.reminderHour, minute: Int = repo.reminderMinute) =
        viewModelScope.launch {
            io {
                repo.reminderEnabled = enabled
                repo.reminderHour = hour
                repo.reminderMinute = minute
                Reminders.applyFromSettings(getApplication(), repo)
            }
            toast = if (enabled) {
                "毎日 %02d:%02d に通知します".format(hour, minute)
            } else {
                "通知を止めました"
            }
            refresh()
        }

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

    fun startReview(deckId: Long?, exam: Boolean = false, noteId: Long? = null) = viewModelScope.launch {
        busy = true
        val name = when {
            noteId != null -> "今日の1問"
            exam -> "試験日から逆算"
            deckId != null -> io { repo.deck(deckId) }?.name ?: "デッキ"
            else -> "すべてのデッキ"
        }
        val queue = when {
            noteId != null -> io { repo.queueForNote(noteId) }
            exam -> io { repo.buildExamQueue() }
            else -> io { repo.buildQueue(deckId) }
        }
        // Worked out once for the whole session rather than per card.
        val stumbles = io { repo.missCounts() }
        forgetUndo()
        session = ReviewSession(
            deckId = deckId, deckName = name, queue = queue, examMode = exam, stumbles = stumbles,
        )
        advance(0)
        busy = false
    }

    fun endReview() {
        forgetUndo()
        session = null
        refresh()
    }

    private fun forgetUndo() {
        undoPoint = null
        canUndo = false
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
        // Taken before the answer, because that is the only copy of the card's state
        // that FSRS cannot reconstruct afterwards.
        undoPoint = UndoPoint(rendered.card, s)
        canUndo = true
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

    /**
     * Take back the last grade.
     *
     * The card goes back to the schedule it had, the review disappears from the log,
     * and the session returns to that card with the answer still showing — so a
     * mis-tap costs a second rather than corrupting an interval for months.
     */
    fun undoLastAnswer() = viewModelScope.launch {
        val point = undoPoint ?: return@launch
        io { repo.undoAnswer(point.card) }
        forgetUndo()
        session = point.session
        toast = "直前の解答を取り消しました"
    }

    /** Skip without grading. */
    fun skip() = viewModelScope.launch {
        val s = session ?: return@launch
        forgetUndo()
        advance(s.position + 1)
    }

    fun suspendCurrent() = viewModelScope.launch {
        val s = session ?: return@launch
        val card = s.current?.card ?: return@launch
        io { repo.setSuspended(card.id, true) }
        forgetUndo()
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

    suspend fun graph(deckId: Long?, subject: Subject? = null): GraphData =
        io { repo.graph(deckId, subject) }

    /** Which subjects the collection actually holds, in the order they are defined. */
    suspend fun subjects(): List<Subject> = io {
        val present = repo.listDecks().map { it.noteType.subject }.toSet()
        Subject.entries.filter { it in present }
    }

    // ---- 速読 -----------------------------------------------------------------

    suspend fun readingPassages(): List<Note> = io { repo.readingPassages() }

    suspend fun readingProgress(): ReadingProgress = io { repo.readingProgress() }

    suspend fun lastReading(noteId: Long): ReadingRecord? = io { repo.lastReading(noteId) }

    fun recordReading(
        noteId: Long,
        tookMs: Long,
        words: Int,
        understood: Boolean,
        onDone: () -> Unit = {},
    ) = viewModelScope.launch {
        io { repo.recordReading(noteId, tookMs, words, understood) }
        onDone()
    }

    // ---- the word shelf ------------------------------------------------------

    suspend fun wordShelf(): List<RootShelf> = io { repo.wordShelf() }

    /**
     * Study one group of words on its own, whatever their due dates.
     *
     * The daily queue decides what is due; this is for when the learner has looked at
     * the shelf and decided that this root is the weak spot.
     */
    fun startGroupReview(name: String, noteIds: List<Long>, onReady: () -> Unit = {}) =
        viewModelScope.launch {
            busy = true
            val queue = io { repo.queueForNotes(noteIds) }
            forgetUndo()
            session = ReviewSession(
                deckId = null,
                deckName = name,
                queue = queue,
                stumbles = io { repo.missCounts() },
            )
            advance(0)
            busy = false
            onReady()
        }

    // ---- 今日の1問 -----------------------------------------------------------

    var problemOfTheDay by mutableStateOf<Note?>(null); private set

    fun loadProblemOfTheDay() = viewModelScope.launch {
        problemOfTheDay = io { repo.problemOfTheDay() }
    }

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

    /** Cards answered wrong over and over — worth changing the approach to. */
    suspend fun leeches(): List<Leech> = io { repo.leeches() }

    fun suspendCard(cardId: Long, onDone: () -> Unit = {}) = viewModelScope.launch {
        io { repo.setSuspended(cardId, true) }
        toast = "このカードを保留しました"
        refresh()
        onDone()
    }

    // ---- the university database --------------------------------------------

    var departments by mutableStateOf<List<Department>>(emptyList()); private set
    var japanMap by mutableStateOf(JapanMap.Empty); private set
    var shortlist by mutableStateOf<Set<String>>(emptySet()); private set
    var universitiesLoading by mutableStateOf(false); private set

    /** Read the bundled tables once; both are small enough to keep in memory. */
    fun loadUniversities() = viewModelScope.launch {
        if (departments.isNotEmpty()) return@launch
        universitiesLoading = true
        val assets = getApplication<Application>().assets
        departments = io {
            assets.open(Universities.ASSET).bufferedReader()
                .useLines { Universities.parse(it) }
        }
        japanMap = io {
            assets.open(JapanMapLoader.ASSET).bufferedReader()
                .useLines { JapanMapLoader.parse(it) }
        }
        shortlist = io { repo.shortlist }
        universitiesLoading = false
    }

    fun toggleShortlist(department: Department) = viewModelScope.launch {
        val added = io { repo.toggleShortlist(department.key) }
        shortlist = io { repo.shortlist }
        toast = if (added) "志望校リストに追加しました" else "志望校リストから外しました"
    }

    companion object {
        private const val REQUEUE_HORIZON_MS = 20 * 60_000L
        private const val REQUEUE_GAP = 8

        /** Mix-ups needed before a pair is linked without being asked. */
        private const val CONFUSION_LINK_THRESHOLD = 2
    }
}
