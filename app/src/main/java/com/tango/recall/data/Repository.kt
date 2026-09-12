package com.tango.recall.data

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.tango.recall.srs.CardPhase
import com.tango.recall.srs.Fsrs
import com.tango.recall.srs.FsrsScheduler
import com.tango.recall.srs.Rating
import java.util.Calendar

data class RelationGroup(
    val type: LinkType,
    val reverse: Boolean,
    val partners: List<RelatedNote>,
)

/** The scheduling state of one card, enough to predict its recall at any time. */
data class CardMemory(val stability: Double, val lastReview: Long?)

/** One note as drawn on the connection map. */
data class GraphNode(
    val noteId: Long,
    val deckId: Long,
    val typeId: String,
    val title: String,
    val subtitle: String,
    val degree: Int,
    /** Mean predicted recall across this note's cards, 0..1. Drives how brightly it glows. */
    val strength: Double,
    val isNew: Boolean,
    /**
     * Kept so the map can be re-evaluated at a future date without touching the
     * database again — that is what the time slider scrubs through.
     */
    val memory: List<CardMemory> = emptyList(),
) {
    /** Mean predicted recall of this note's cards at [at]. */
    fun strengthAt(at: Long): Double {
        if (memory.isEmpty()) return 0.0
        return memory.map { card ->
            val last = card.lastReview ?: return@map 0.0
            Fsrs.recallAfter((at - last).toDouble() / 86_400_000.0, card.stability)
        }.average()
    }
}

data class GraphEdge(val from: Long, val to: Long, val typeId: String, val label: String)

data class GraphData(val nodes: List<GraphNode>, val edges: List<GraphEdge>)

data class DeckCounts(val newCount: Int, val learnCount: Int, val dueCount: Int, val total: Int) {
    val studyable: Int get() = newCount + learnCount + dueCount
}

data class ExamOutlook(
    val daysLeft: Int,
    val studiedCards: Int,
    val untouchedCards: Int,
    /** Mean predicted recall across studied cards on the exam date. */
    val predictedMean: Double,
    val atRisk: Int,
    val effectiveRetention: Double,
    val weakest: List<Pair<Note, Double>>,
)

data class ConfusionPair(val a: Note, val b: Note, val times: Int, val linked: Boolean)

/** A card that keeps being answered wrong, with how often it has been missed. */
data class Leech(val note: Note, val card: Card, val misses: Int) {
    val label: String get() = cardLabel(note, card.templateId)
}

data class Stats(
    val totalNotes: Int,
    val totalCards: Int,
    val reviewsToday: Int,
    val correctToday: Int,
    val streakDays: Int,
    val dueNext7Days: List<Int>,
    val matureCards: Int,
    val averageStability: Double,
)

/** Hour at which a new "study day" starts, so late-night sessions count as yesterday. */
private const val ROLLOVER_HOUR = 4

fun dayStart(now: Long, daysAgo: Int = 0): Long {
    val c = Calendar.getInstance().apply {
        timeInMillis = now
        if (get(Calendar.HOUR_OF_DAY) < ROLLOVER_HOUR) add(Calendar.DAY_OF_YEAR, -1)
        set(Calendar.HOUR_OF_DAY, ROLLOVER_HOUR)
        set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        add(Calendar.DAY_OF_YEAR, -daysAgo)
    }
    return c.timeInMillis
}

class Repository(private val helper: TangoDb) {

    private val db: SQLiteDatabase get() = helper.writableDatabase

    // ---- settings -----------------------------------------------------------

    fun setting(key: String, default: String): String =
        db.rawQuery("SELECT v FROM settings WHERE k=?", arrayOf(key)).use {
            if (it.moveToFirst()) it.getString(0) else default
        }

    fun putSetting(key: String, value: String) {
        db.insertWithOnConflict(
            "settings", null,
            ContentValues().apply { put("k", key); put("v", value) },
            SQLiteDatabase.CONFLICT_REPLACE,
        )
    }

    var desiredRetention: Double
        get() = setting(KEY_RETENTION, "0.90").toDoubleOrNull()?.coerceIn(0.70, 0.98) ?: 0.90
        set(value) = putSetting(KEY_RETENTION, value.coerceIn(0.70, 0.98).toString())

    var showRelated: Boolean
        get() = setting(KEY_SHOW_RELATED, "true").toBoolean()
        set(value) = putSetting(KEY_SHOW_RELATED, value.toString())

    var maxReviewsPerDay: Int
        get() = setting(KEY_MAX_REVIEWS, "200").toIntOrNull() ?: 200
        set(value) = putSetting(KEY_MAX_REVIEWS, value.coerceIn(10, 9999).toString())

    /** Whether the daily reminder is armed, and when it goes off. */
    var reminderEnabled: Boolean
        get() = setting(KEY_REMINDER, "false").toBoolean()
        set(value) = putSetting(KEY_REMINDER, value.toString())

    var reminderHour: Int
        get() = setting(KEY_REMINDER_HOUR, "20").toIntOrNull()?.coerceIn(0, 23) ?: 20
        set(value) = putSetting(KEY_REMINDER_HOUR, value.coerceIn(0, 23).toString())

    var reminderMinute: Int
        get() = setting(KEY_REMINDER_MINUTE, "0").toIntOrNull()?.coerceIn(0, 59) ?: 0
        set(value) = putSetting(KEY_REMINDER_MINUTE, value.coerceIn(0, 59).toString())

    /** Epoch millis of the exam being prepared for, or 0 when none is set. */
    var examDate: Long
        get() = setting(KEY_EXAM_DATE, "0").toLongOrNull() ?: 0L
        set(value) = putSetting(KEY_EXAM_DATE, value.coerceAtLeast(0L).toString())

    fun daysUntilExam(now: Long = System.currentTimeMillis()): Int? {
        val exam = examDate
        if (exam <= 0L) return null
        return Math.ceil((exam - now).toDouble() / 86_400_000.0).toInt()
    }

    /**
     * The retention target actually used for scheduling.
     *
     * With an exam set, this ramps from the learner's baseline up towards [EXAM_PEAK_RETENTION]
     * over the final [RAMP_DAYS] days. Reviewing at a higher target shortens intervals, so
     * material naturally gets tighter as the date approaches instead of needing a panic
     * week of cramming. Outside that window nothing changes.
     */
    fun effectiveRetention(now: Long = System.currentTimeMillis()): Double {
        val base = desiredRetention
        val daysLeft = daysUntilExam(now) ?: return base
        if (daysLeft > RAMP_DAYS || daysLeft < 0) return base
        val progress = 1.0 - daysLeft.toDouble() / RAMP_DAYS
        return (base + (EXAM_PEAK_RETENTION - base) * progress).coerceIn(base, EXAM_PEAK_RETENTION)
    }

    /**
     * Which packs of bundled content are already in.
     *
     * Content ships in packs so that a later version can add material to a phone that
     * was set up long ago: anything not listed here is installed on the next launch.
     */
    var installedSeedPacks: Set<String>
        get() = setting(KEY_SEED_PACKS, "[]").toStringSet()
        set(value) = putSetting(KEY_SEED_PACKS, value.toJsonArray())

    /** Departments the learner has shortlisted, by [Department.key]. */
    var shortlist: Set<String>
        get() = setting(KEY_SHORTLIST, "[]").toStringSet()
        set(value) = putSetting(KEY_SHORTLIST, value.toJsonArray())

    fun toggleShortlist(key: String): Boolean {
        val current = shortlist
        val added = key !in current
        shortlist = if (added) current + key else current - key
        return added
    }

    fun scheduler(now: Long = System.currentTimeMillis()): FsrsScheduler =
        FsrsScheduler(desiredRetention = effectiveRetention(now))

    // ---- decks --------------------------------------------------------------

    fun listDecks(): List<Deck> =
        db.rawQuery("SELECT * FROM decks ORDER BY created", null).mapAll { it.toDeck() }

    fun deck(id: Long): Deck? =
        db.rawQuery("SELECT * FROM decks WHERE id=?", arrayOf(id.toString())).mapAll { it.toDeck() }
            .firstOrNull()

    fun saveDeck(deck: Deck): Long {
        val cv = ContentValues().apply {
            put("name", deck.name)
            put("noteType", deck.noteTypeId)
            put("enabledTemplates", deck.enabledTemplates.toJsonArray())
            put("newPerDay", deck.newPerDay)
            put("relationQuiz", if (deck.relationQuiz) 1 else 0)
            put("created", deck.created)
        }
        return if (deck.id == 0L) {
            db.insert("decks", null, cv)
        } else {
            db.update("decks", cv, "id=?", arrayOf(deck.id.toString()))
            // Enabling/disabling a direction must add or remove the matching cards.
            // One transaction for the whole deck: otherwise every statement commits on
            // its own and saving a large deck takes seconds.
            val notes = listNotes(deck.id, "", Int.MAX_VALUE)
            transaction { notes.forEach { regenerateCards(it, deck) } }
            deck.id
        }
    }

    fun deleteDeck(id: Long) {
        db.delete("decks", "id=?", arrayOf(id.toString()))
    }

    fun deckCounts(now: Long = System.currentTimeMillis()): Map<Long, DeckCounts> {
        val result = mutableMapOf<Long, DeckCounts>()
        for (deck in listDecks()) {
            val introduced = introducedToday(deck.id, now)
            val newAvailable = countCards(deck.id, "phase='NEW' AND suspended=0")
            val newCount = minOf(newAvailable, (deck.newPerDay - introduced).coerceAtLeast(0))
            val learn = countCards(deck.id, "phase IN ('LEARNING','RELEARNING') AND suspended=0 AND due<=$now")
            val due = countCards(deck.id, "phase='REVIEW' AND suspended=0 AND due<=$now")
            val total = countCards(deck.id, "1=1")
            result[deck.id] = DeckCounts(newCount, learn, due, total)
        }
        return result
    }

    private fun countCards(deckId: Long, where: String): Int =
        db.rawQuery("SELECT COUNT(*) FROM cards WHERE deckId=? AND ($where)", arrayOf(deckId.toString()))
            .use { if (it.moveToFirst()) it.getInt(0) else 0 }

    private fun introducedToday(deckId: Long, now: Long): Int =
        db.rawQuery(
            "SELECT COUNT(DISTINCT cardId) FROM reviews WHERE deckId=? AND ts>=? AND phase='NEW'",
            arrayOf(deckId.toString(), dayStart(now).toString()),
        ).use { if (it.moveToFirst()) it.getInt(0) else 0 }

    // ---- notes --------------------------------------------------------------

    fun listNotes(
        deckId: Long?,
        query: String = "",
        limit: Int = 500,
        subject: Subject? = null,
    ): List<Note> {
        val args = mutableListOf<String>()
        val where = buildString {
            append("1=1")
            if (deckId != null) { append(" AND deckId=?"); args += deckId.toString() }
            if (subject != null) {
                val types = NoteType.entries.filter { it.subject == subject }
                append(types.joinToString(",", " AND type IN (", ")") { "?" })
                args += types.map { it.id }
            }
            if (query.isNotBlank()) {
                // The note's own search text, not the raw JSON: that matched field
                // *names* ("memo" found every note) and missed anything org.json
                // escapes, "mol/L" among them.
                append(" AND search LIKE ? ESCAPE '\\'")
                args += "%${query.trim().lowercase().escapeLikeArgument()}%"
            }
        }
        return db.rawQuery(
            "SELECT * FROM notes WHERE $where ORDER BY modified DESC LIMIT $limit",
            args.toTypedArray(),
        ).mapAll { it.toNote() }
    }

    fun note(id: Long): Note? =
        db.rawQuery("SELECT * FROM notes WHERE id=?", arrayOf(id.toString())).mapAll { it.toNote() }
            .firstOrNull()

    fun notes(ids: Collection<Long>): Map<Long, Note> {
        if (ids.isEmpty()) return emptyMap()
        val placeholders = ids.joinToString(",") { "?" }
        return db.rawQuery(
            "SELECT * FROM notes WHERE id IN ($placeholders)",
            ids.map { it.toString() }.toTypedArray(),
        ).mapAll { it.toNote() }.associateBy { it.id }
    }

    /** Insert or update a note and bring its card set back in sync with its content. */
    fun saveNote(note: Note): Long {
        val now = System.currentTimeMillis()
        val cv = ContentValues().apply {
            put("deckId", note.deckId)
            put("type", note.typeId)
            put("fields", note.fields.toJson())
            put("tags", note.tags.tagsToDb())
            put("search", searchText(note.fields, note.tags))
            put("created", if (note.id == 0L) now else note.created)
            put("modified", now)
        }
        val id = if (note.id == 0L) {
            db.insert("notes", null, cv)
        } else {
            db.update("notes", cv, "id=?", arrayOf(note.id.toString())); note.id
        }
        deck(note.deckId)?.let { regenerateCards(note.copy(id = id), it) }
        return id
    }

    fun deleteNote(id: Long) {
        db.delete("notes", "id=?", arrayOf(id.toString()))
    }

    /**
     * Create cards for every enabled direction whose required fields are filled,
     * and drop cards whose direction no longer applies.
     */
    private fun regenerateCards(note: Note, deck: Deck) {
        val fromFields = note.type.templates
            .filter { it.id in deck.enabledTemplates }
            .filter { tpl -> tpl.requires.all { note[it].isNotBlank() } }
            .map { it.id }
        val fromRelations = if (deck.relationQuiz) relationGroups(note.id).keys else emptySet()
        val wanted = (fromFields + fromRelations).toSet()

        val existing = db.rawQuery(
            "SELECT * FROM cards WHERE noteId=?", arrayOf(note.id.toString()),
        ).mapAll { it.toCard() }
        val existingIds = existing.map { it.templateId }.toSet()

        for (tplId in wanted - existingIds) {
            db.insertWithOnConflict(
                "cards", null,
                Card(noteId = note.id, deckId = note.deckId, templateId = tplId).toValues(),
                SQLiteDatabase.CONFLICT_IGNORE,
            )
        }
        for (card in existing) {
            val id = arrayOf<Any>(card.id)
            when {
                // The direction applies again — bring back what the app had put away,
                // with its schedule intact. A card suspended by hand stays suspended.
                card.templateId in wanted ->
                    if (card.autoSuspended) {
                        db.execSQL("UPDATE cards SET suspended=0, autoSuspended=0 WHERE id=?", id)
                    }

                // Never studied: there is nothing to preserve, so drop it.
                card.srs.reps == 0 && card.srs.phase == CardPhase.NEW ->
                    db.delete("cards", "id=?", arrayOf(card.id.toString()))

                // Studied: turning a direction off, or clearing the field it asks
                // about, must not throw away weeks of review history.
                !card.suspended ->
                    db.execSQL("UPDATE cards SET suspended=1, autoSuspended=1 WHERE id=?", id)
            }
        }
        // A note can be moved between decks; keep its cards pointing at the right one.
        db.execSQL("UPDATE cards SET deckId=? WHERE noteId=?", arrayOf(note.deckId, note.id))
    }

    /**
     * The relation questions this note can currently be asked, keyed by card template id.
     *
     * Partners are grouped so one card covers every note reachable by the same
     * relation in the same direction.
     */
    fun relationGroups(noteId: Long): Map<String, RelationGroup> {
        val grouped = LinkedHashMap<String, MutableList<RelatedNote>>()
        val labels = HashMap<String, Pair<LinkType, Boolean>>()
        for (rel in related(noteId)) {
            val type = rel.link.type
            val reverse = !type.symmetric && rel.link.toNoteId == noteId
            val id = RelationCards.templateId(type, reverse)
            grouped.getOrPut(id) { mutableListOf() } += rel
            labels[id] = type to reverse
        }
        return grouped.mapValues { (id, partners) ->
            val (type, reverse) = labels.getValue(id)
            RelationGroup(type, reverse, partners)
        }
    }

    /** Render any card, including the ones generated from the relation graph. */
    fun renderAnyCard(card: Card, note: Note): RenderedCard? {
        if (!RelationCards.isRelationCard(card.templateId)) return renderCard(card, note)
        val group = relationGroups(note.id)[card.templateId] ?: return null
        return renderRelationCard(card, note, group.type, group.reverse, group.partners)
    }

    /** Rebuild the relation cards of one note after its links changed. */
    private fun refreshRelationCards(noteId: Long) {
        val note = note(noteId) ?: return
        val deck = deck(note.deckId) ?: return
        if (deck.relationQuiz) regenerateCards(note, deck)
    }

    fun cardsOfNote(noteId: Long): List<Card> =
        db.rawQuery("SELECT * FROM cards WHERE noteId=? ORDER BY id", arrayOf(noteId.toString()))
            .mapAll { it.toCard() }

    /**
     * The cards of many notes in one query.
     *
     * The connection map and the backup both walk every note; asking per note made
     * that one query per note, which is what made opening the map slow.
     */
    fun cardsOfNotes(noteIds: Collection<Long>): Map<Long, List<Card>> {
        if (noteIds.isEmpty()) return emptyMap()
        return db.rawQuery(
            "SELECT * FROM cards WHERE noteId IN (${noteIds.joinToString(",")}) ORDER BY id", null,
        ).mapAll { it.toCard() }.groupBy { it.noteId }
    }

    fun card(id: Long): Card? =
        db.rawQuery("SELECT * FROM cards WHERE id=?", arrayOf(id.toString())).mapAll { it.toCard() }
            .firstOrNull()

    /** Suspend or resume by hand. The learner's choice outranks the automatic one. */
    fun setSuspended(cardId: Long, suspended: Boolean) {
        db.execSQL(
            "UPDATE cards SET suspended=?, autoSuspended=0 WHERE id=?",
            arrayOf(if (suspended) 1 else 0, cardId),
        )
    }

    /**
     * One problem to work through today, the same one all day.
     *
     * Chosen by the date rather than at random so that it does not change under the
     * learner between one glance and the next, and so that "今日の1問" means the same
     * thing on two devices. It rotates through everything problem-shaped — a
     * calculation, a translation, a plan for a maths question — because that is what
     * the second-stage paper actually asks for.
     */
    fun problemOfTheDay(now: Long = System.currentTimeMillis()): Note? {
        val types = PROBLEM_TYPES.joinToString(",") { "'" + it.id + "'" }
        val ids = db.rawQuery("SELECT id FROM notes WHERE type IN ($types) ORDER BY id", null)
            .mapAll { it.getLong(0) }
        if (ids.isEmpty()) return null
        val day = Math.floorDiv(dayStart(now), 86_400_000L)
        val index = Math.floorMod(day, ids.size.toLong()).toInt()
        return note(ids[index])
    }

    /** Just this note's cards, for studying one item on its own. */
    fun queueForNote(noteId: Long): List<Long> =
        cardsOfNote(noteId).filterNot { it.suspended }.map { it.id }

    // ---- study queue --------------------------------------------------------

    /**
     * Build today's queue for [deckId] (or every deck when null).
     *
     * New cards are spread through the due cards rather than front-loaded, and two
     * cards from the same note are pushed apart — interleaving different material
     * produces better retention than studying one block at a time.
     */
    fun buildQueue(deckId: Long?, now: Long = System.currentTimeMillis()): List<Long> {
        val deckFilter = deckId?.let { " AND deckId=$it" } ?: ""

        val learning = db.rawQuery(
            "SELECT * FROM cards WHERE suspended=0 AND phase IN ('LEARNING','RELEARNING') AND due<=?$deckFilter ORDER BY due",
            arrayOf(now.toString()),
        ).mapAll { it.toCard() }

        val reviews = db.rawQuery(
            "SELECT * FROM cards WHERE suspended=0 AND phase='REVIEW' AND due<=?$deckFilter ORDER BY due LIMIT ${maxReviewsPerDay}",
            arrayOf(now.toString()),
        ).mapAll { it.toCard() }

        val decks = if (deckId != null) listOfNotNull(deck(deckId)) else listDecks()
        val fresh = decks.flatMap { d ->
            val allowance = (d.newPerDay - introducedToday(d.id, now)).coerceAtLeast(0)
            if (allowance == 0) emptyList() else db.rawQuery(
                "SELECT * FROM cards WHERE suspended=0 AND phase='NEW' AND deckId=? ORDER BY noteId, id LIMIT $allowance",
                arrayOf(d.id.toString()),
            ).mapAll { it.toCard() }
        }

        val backlog = (learning + reviews).sortedBy { it.srs.due }
        return spaceSiblings(interleave(backlog, fresh)).map { it.id }
    }

    /** Distribute [fresh] evenly through [backlog] instead of clumping them. */
    private fun interleave(backlog: List<Card>, fresh: List<Card>): List<Card> {
        if (fresh.isEmpty()) return backlog
        if (backlog.isEmpty()) return fresh
        val out = ArrayList<Card>(backlog.size + fresh.size)
        val gap = (backlog.size.toDouble() / fresh.size).coerceAtLeast(1.0)
        var nextAt = 0.0
        var f = 0
        backlog.forEachIndexed { i, card ->
            while (f < fresh.size && i >= nextAt) {
                out += fresh[f]; f++; nextAt += gap
            }
            out += card
        }
        while (f < fresh.size) { out += fresh[f]; f++ }
        return out
    }

    /**
     * Keep two directions of the same note from appearing back to back.
     *
     * Among the candidates that are far enough from their last sibling, prefer the
     * one with the most cards still queued; otherwise a note whose siblings all sit
     * at the end of the list gets stranded there and ends up bunched together.
     */
    private fun spaceSiblings(cards: List<Card>, minGap: Int = 4, lookahead: Int = 16): List<Card> {
        if (cards.size <= 2) return cards
        val remaining = cards.groupingBy { it.noteId }.eachCount().toMutableMap()
        // With very few distinct notes a wide gap is impossible to honour; shrink it
        // rather than falling back to an arbitrary pick.
        val gap = minOf(minGap, remaining.size - 1).coerceAtLeast(0)

        val pending = ArrayList(cards)
        val out = ArrayList<Card>(cards.size)
        val recent = ArrayDeque<Long>()

        while (pending.isNotEmpty()) {
            var bestIndex = -1
            var bestCount = -1
            var seen = 0
            for (i in pending.indices) {
                if (pending[i].noteId in recent) continue
                val count = remaining[pending[i].noteId] ?: 0
                if (count > bestCount) { bestCount = count; bestIndex = i }
                if (++seen >= lookahead) break
            }
            if (bestIndex < 0) bestIndex = 0
            val pick = pending.removeAt(bestIndex)
            out += pick
            remaining[pick.noteId] = (remaining[pick.noteId] ?: 1) - 1
            recent.addLast(pick.noteId)
            if (recent.size > gap) recent.removeFirst()
        }
        return out
    }

    /** Grade a card, persist the new schedule and append to the review log. */
    fun answer(card: Card, rating: Rating, now: Long = System.currentTimeMillis(), tookMs: Long = 0): Card {
        val phaseBefore = card.srs.phase
        val newState = scheduler(now).review(card.srs, rating, now)
        val updated = card.copy(srs = newState)
        db.update("cards", updated.toValues(), "id=?", arrayOf(card.id.toString()))
        db.insert(
            "reviews", null,
            ContentValues().apply {
                put("cardId", card.id)
                put("noteId", card.noteId)
                put("deckId", card.deckId)
                put("rating", rating.value)
                put("ts", now)
                put("phase", phaseBefore.name)
                put("stability", newState.stability)
                put("difficulty", newState.difficulty)
                put("tookMs", tookMs)
            },
        )
        return updated
    }

    /**
     * Put a card back exactly as it was and drop the review that moved it.
     *
     * A mis-tapped button otherwise rewrites the card's stability and difficulty for
     * good — there is no way to tell FSRS "that grade was not true", so the state has
     * to be restored wholesale from the copy taken before the answer.
     */
    fun undoAnswer(previous: Card) {
        db.update("cards", previous.toValues(), "id=?", arrayOf(previous.id.toString()))
        db.execSQL(
            "DELETE FROM reviews WHERE id=(SELECT MAX(id) FROM reviews WHERE cardId=?)",
            arrayOf(previous.id),
        )
    }

    // ---- links --------------------------------------------------------------

    fun related(noteId: Long): List<RelatedNote> {
        val links = db.rawQuery(
            "SELECT * FROM links WHERE fromNoteId=? OR toNoteId=?",
            arrayOf(noteId.toString(), noteId.toString()),
        ).mapAll { it.toLink() }
        if (links.isEmpty()) return emptyList()
        val others = notes(links.map { if (it.fromNoteId == noteId) it.toNoteId else it.fromNoteId })
        return links.mapNotNull { link ->
            val otherId = if (link.fromNoteId == noteId) link.toNoteId else link.fromNoteId
            val other = others[otherId] ?: return@mapNotNull null
            val label = if (link.fromNoteId == noteId) link.type.forward else link.type.reverse
            RelatedNote(link, other, label)
        }.sortedBy { it.label }
    }

    fun addLink(fromNoteId: Long, toNoteId: Long, type: LinkType, memo: String = ""): Boolean {
        if (fromNoteId == toNoteId) return false
        val id = db.insertWithOnConflict(
            "links", null,
            ContentValues().apply {
                put("fromNoteId", fromNoteId)
                put("toNoteId", toNoteId)
                put("type", type.id)
                put("memo", memo)
                put("created", System.currentTimeMillis())
            },
            SQLiteDatabase.CONFLICT_IGNORE,
        )
        if (id == -1L) return false
        // Both endpoints gain a relation question, or gain a partner in an existing one.
        refreshRelationCards(fromNoteId)
        refreshRelationCards(toNoteId)
        return true
    }

    fun deleteLink(id: Long) {
        val link = db.rawQuery("SELECT * FROM links WHERE id=?", arrayOf(id.toString()))
            .mapAll { it.toLink() }.firstOrNull()
        db.delete("links", "id=?", arrayOf(id.toString()))
        link?.let {
            refreshRelationCards(it.fromNoteId)
            refreshRelationCards(it.toNoteId)
        }
    }

    /**
     * Notes that plausibly belong next to [note]: same etymological root, or a shared
     * tag. Offered as one-tap link suggestions in the editor.
     */
    fun suggestLinks(note: Note, limit: Int = 12): List<Pair<Note, LinkType>> {
        val already = related(note.id).map { it.other.id }.toSet() + note.id
        val out = LinkedHashMap<Long, Pair<Note, LinkType>>()

        val root = note["root"].trim()
        if (root.isNotBlank()) {
            db.rawQuery(
                "SELECT * FROM notes WHERE type=? AND fields LIKE ? LIMIT 40",
                arrayOf(note.typeId, "%${root.escapeLike()}%"),
            ).mapAll { it.toNote() }
                .filter { it.id !in already && it["root"].trim() == root }
                .forEach { out.putIfAbsent(it.id, it to LinkType.SAME_ROOT) }
        }

        for (tag in note.tags) {
            db.rawQuery(
                "SELECT * FROM notes WHERE tags LIKE ? LIMIT 40", arrayOf("%$tag%"),
            ).mapAll { it.toNote() }
                .filter { it.id !in already && tag in it.tags }
                .forEach { out.putIfAbsent(it.id, it to LinkType.SAME_GROUP) }
        }

        val category = note["category"].trim()
        if (category.isNotBlank()) {
            db.rawQuery(
                "SELECT * FROM notes WHERE type=? LIMIT 200", arrayOf(note.typeId),
            ).mapAll { it.toNote() }
                .filter { it.id !in already && it["category"].trim() == category }
                .forEach { out.putIfAbsent(it.id, it to LinkType.SAME_GROUP) }
        }

        return out.values.take(limit)
    }

    private fun String.escapeLike() = replace("%", "").replace("_", "")

    /** Quote the wildcards so a search for "50%" looks for a per-cent sign. */
    private fun String.escapeLikeArgument() =
        replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

    // ---- stats --------------------------------------------------------------

    fun stats(now: Long = System.currentTimeMillis()): Stats {
        fun scalarInt(sql: String, args: Array<String> = emptyArray()) =
            db.rawQuery(sql, args).use { if (it.moveToFirst()) it.getInt(0) else 0 }

        val todayStart = dayStart(now)
        val reviewsToday = scalarInt("SELECT COUNT(*) FROM reviews WHERE ts>=?", arrayOf(todayStart.toString()))
        val correctToday =
            scalarInt("SELECT COUNT(*) FROM reviews WHERE ts>=? AND rating>1", arrayOf(todayStart.toString()))

        val forecast = (0..6).map { d ->
            val from = if (d == 0) 0L else dayStart(now, -d)
            val to = dayStart(now, -(d + 1))
            scalarInt(
                "SELECT COUNT(*) FROM cards WHERE suspended=0 AND phase!='NEW' AND due>=? AND due<?",
                arrayOf(from.toString(), to.toString()),
            )
        }

        val avgStability = db.rawQuery(
            "SELECT AVG(stability) FROM cards WHERE phase='REVIEW'", null,
        ).use { if (it.moveToFirst() && !it.isNull(0)) it.getDouble(0) else 0.0 }

        return Stats(
            totalNotes = scalarInt("SELECT COUNT(*) FROM notes"),
            totalCards = scalarInt("SELECT COUNT(*) FROM cards"),
            reviewsToday = reviewsToday,
            correctToday = correctToday,
            streakDays = studyStreak(now),
            dueNext7Days = forecast,
            matureCards = scalarInt("SELECT COUNT(*) FROM cards WHERE phase='REVIEW' AND stability>=21"),
            averageStability = avgStability,
        )
    }

    /**
     * Consecutive days studied, ending today — or yesterday, if today's session has
     * not happened yet.
     *
     * Walks back from the most recent review instead of asking about each of the last
     * 365 days in turn: one query per day actually studied, and it stops at the first
     * gap rather than always running the whole year.
     */
    internal fun studyStreak(now: Long = System.currentTimeMillis()): Int {
        val today = dayStart(now)
        var streak = 0
        var expected = 0
        var boundary = dayStart(now, -1)
        while (streak <= 365) {
            val last = db.rawQuery(
                "SELECT MAX(ts) FROM reviews WHERE ts<?", arrayOf(boundary.toString()),
            ).use { if (it.moveToFirst() && !it.isNull(0)) it.getLong(0) else null } ?: break
            val daysAgo = Math.round((today - dayStart(last)).toDouble() / 86_400_000.0).toInt()
            // Not having studied yet today does not break a streak; a missed day does.
            if (daysAgo != expected && !(streak == 0 && daysAgo == 1)) break
            streak++
            expected = daysAgo + 1
            boundary = dayStart(now, daysAgo)
        }
        return streak
    }

    /** Cards whose predicted recall has already dropped below [threshold]. */
    fun weakest(limit: Int = 20, now: Long = System.currentTimeMillis(), threshold: Double = 0.9): List<Pair<Note, Double>> {
        val sched = scheduler()
        val cards = db.rawQuery(
            "SELECT * FROM cards WHERE phase='REVIEW' AND suspended=0 LIMIT 2000", null,
        ).mapAll { it.toCard() }
        val scored = cards.map { it to sched.retrievability(it.srs, now) }
            .filter { it.second < threshold }
            .sortedBy { it.second }
            .take(limit)
        val ns = notes(scored.map { it.first.noteId })
        return scored.mapNotNull { (c, r) -> ns[c.noteId]?.let { it to r } }
    }

    /**
     * The whole relation graph, ready to lay out.
     *
     * Node strength is the mean predicted recall of the note's cards, so the map
     * doubles as a picture of what is currently solid and what is fading.
     */
    fun graph(
        deckId: Long?,
        subject: Subject? = null,
        now: Long = System.currentTimeMillis(),
        limit: Int = 900,
    ): GraphData {
        val notes = listNotes(deckId, "", limit, subject)
        if (notes.isEmpty()) return GraphData(emptyList(), emptyList())
        val ids = notes.map { it.id }.toSet()
        val sched = scheduler()

        val allLinks = db.rawQuery("SELECT * FROM links", null).mapAll { it.toLink() }
            .filter { it.fromNoteId in ids && it.toNoteId in ids }

        val degree = HashMap<Long, Int>()
        for (link in allLinks) {
            degree[link.fromNoteId] = (degree[link.fromNoteId] ?: 0) + 1
            degree[link.toNoteId] = (degree[link.toNoteId] ?: 0) + 1
        }

        val cardsByNote = cardsOfNotes(ids)
        val nodes = notes.map { note ->
            val cards = cardsByNote[note.id].orEmpty()
            val seen = cards.filter { it.srs.phase != CardPhase.NEW }
            GraphNode(
                noteId = note.id,
                deckId = note.deckId,
                typeId = note.typeId,
                title = note.title(),
                subtitle = note.subtitle(),
                degree = degree[note.id] ?: 0,
                strength = if (seen.isEmpty()) 0.0 else seen.map { sched.retrievability(it.srs, now) }.average(),
                isNew = seen.isEmpty(),
                memory = seen.map { CardMemory(it.srs.stability, it.srs.lastReview) },
            )
        }

        val edges = allLinks.map {
            GraphEdge(it.fromNoteId, it.toNoteId, it.typeId, it.type.forward)
        }
        return GraphData(nodes, edges)
    }

    /**
     * What the collection is predicted to look like on exam day.
     *
     * Ordinary spaced repetition asks "is this due today?". With a fixed date to aim
     * at, the more useful question is "what will have decayed by then?" — and that is
     * answered by evaluating the forgetting curve at the exam date rather than now.
     */
    fun examOutlook(now: Long = System.currentTimeMillis(), weakestLimit: Int = 15): ExamOutlook? {
        val exam = examDate
        if (exam <= 0L) return null
        val daysLeft = daysUntilExam(now) ?: return null

        val studied = db.rawQuery(
            "SELECT * FROM cards WHERE suspended=0 AND phase!='NEW'", null,
        ).mapAll { it.toCard() }
        val untouched = db.rawQuery(
            "SELECT COUNT(*) FROM cards WHERE suspended=0 AND phase='NEW'", null,
        ).use { if (it.moveToFirst()) it.getInt(0) else 0 }

        val target = desiredRetention
        val predictions = studied.map { card ->
            val last = card.srs.lastReview
            val predicted = if (last == null) 0.0
            else Fsrs.recallAfter((exam - last).toDouble() / 86_400_000.0, card.srs.stability)
            card to predicted
        }

        val weakest = predictions.sortedBy { it.second }.take(weakestLimit)
        val weakestNotes = notes(weakest.map { it.first.noteId })

        return ExamOutlook(
            daysLeft = daysLeft,
            studiedCards = studied.size,
            untouchedCards = untouched,
            predictedMean = if (predictions.isEmpty()) 0.0 else predictions.map { it.second }.average(),
            atRisk = predictions.count { it.second < target },
            effectiveRetention = effectiveRetention(now),
            weakest = weakest.mapNotNull { (card, r) -> weakestNotes[card.noteId]?.let { it to r } },
        )
    }

    /**
     * A session aimed at the exam rather than at today: the cards predicted weakest on
     * the day, worst first. New cards are deliberately excluded — those are governed by
     * the per-deck daily limit and come through the normal queue.
     */
    fun buildExamQueue(now: Long = System.currentTimeMillis(), limit: Int = maxReviewsPerDay): List<Long> {
        val exam = examDate
        if (exam <= 0L) return emptyList()
        val target = desiredRetention
        return db.rawQuery("SELECT * FROM cards WHERE suspended=0 AND phase!='NEW'", null)
            .mapAll { it.toCard() }
            .map { card ->
                val last = card.srs.lastReview
                card to if (last == null) 0.0
                else Fsrs.recallAfter((exam - last).toDouble() / 86_400_000.0, card.srs.stability)
            }
            .filter { it.second < target }
            .sortedBy { it.second }
            .take(limit)
            .map { it.first.id }
            .let { spaceSiblingsById(it) }
    }

    private fun spaceSiblingsById(ids: List<Long>): List<Long> {
        if (ids.size <= 2) return ids
        val cards = ids.mapNotNull { card(it) }
        return spaceSiblings(cards).map { it.id }
    }

    // ---- cards that keep being missed ---------------------------------------

    /**
     * How often each card has been answered "もう一度", for the cards that keep
     * coming back wrong.
     *
     * Counted from the review log rather than from the card's lapse counter, because
     * a card can be failed over and over while still in learning without the lapse
     * counter ever moving.
     */
    fun missCounts(minimum: Int = LEECH_MISSES): Map<Long, Int> =
        // The threshold is written into the SQL rather than bound: COUNT(*) has no
        // column affinity, so SQLite would compare it against a bound string and never
        // match. It is an Int, so there is nothing to quote.
        db.rawQuery(
            "SELECT cardId, COUNT(*) AS n FROM reviews WHERE rating=1 GROUP BY cardId " +
                "HAVING n>=${minimum.coerceAtLeast(1)}",
            null,
        ).mapAll { it.getLong(0) to it.getInt(1) }.toMap()

    /**
     * The cards to do something about: re-word them, split them, or tie them to
     * something already known. Worst first.
     */
    fun leeches(limit: Int = 10, minimum: Int = LEECH_MISSES): List<Leech> {
        val counts = missCounts(minimum).entries.sortedByDescending { it.value }.take(limit)
        if (counts.isEmpty()) return emptyList()
        val cards = counts.mapNotNull { card(it.key) }
        val notes = notes(cards.map { it.noteId })
        return counts.mapNotNull { (cardId, misses) ->
            val card = cards.firstOrNull { it.id == cardId } ?: return@mapNotNull null
            val note = notes[card.noteId] ?: return@mapNotNull null
            Leech(note, card, misses)
        }
    }

    // ---- confusions ---------------------------------------------------------

    /**
     * The note whose answer was typed instead of the right one, if any.
     *
     * This is what turns a mistake into structure: writing "concede" where "precede"
     * belonged says something specific about your memory that no generic "wrong"
     * flag captures.
     */
    fun findConfusion(note: Note, templateId: String, typed: String): Note? {
        if (typed.trim().length < 2) return null
        val template = note.type.template(templateId) ?: return null
        val field = template.clozeAnswerField ?: template.answerFields.firstOrNull() ?: return null
        val chemistry = note.type == NoteType.CHEM_SUBSTANCE || note.type == NoteType.CHEM_REACTION
        val wanted = normalizeAnswer(typed, chemistry)
        if (wanted.isEmpty()) return null
        if (normalizeAnswer(note[field], chemistry) == wanted) return null

        return db.rawQuery(
            "SELECT * FROM notes WHERE type=? AND id<>? LIMIT 2000",
            arrayOf(note.typeId, note.id.toString()),
        ).mapAll { it.toNote() }
            .firstOrNull { other ->
                other[field].isNotBlank() && normalizeAnswer(other[field], chemistry) == wanted
            }
    }

    /** Log a mix-up and return how many times this pair has now been confused. */
    fun recordConfusion(noteId: Long, otherNoteId: Long, templateId: String, typed: String): Int {
        db.insert(
            "confusions", null,
            ContentValues().apply {
                put("noteId", noteId)
                put("otherNoteId", otherNoteId)
                put("templateId", templateId)
                put("typed", typed)
                put("ts", System.currentTimeMillis())
            },
        )
        return db.rawQuery(
            "SELECT COUNT(*) FROM confusions WHERE (noteId=? AND otherNoteId=?) OR (noteId=? AND otherNoteId=?)",
            arrayOf(
                noteId.toString(), otherNoteId.toString(),
                otherNoteId.toString(), noteId.toString(),
            ),
        ).use { if (it.moveToFirst()) it.getInt(0) else 0 }
    }

    fun areLinked(a: Long, b: Long): Boolean =
        db.rawQuery(
            "SELECT COUNT(*) FROM links WHERE (fromNoteId=? AND toNoteId=?) OR (fromNoteId=? AND toNoteId=?)",
            arrayOf(a.toString(), b.toString(), b.toString(), a.toString()),
        ).use { if (it.moveToFirst()) it.getInt(0) > 0 else false }

    /** Pairs mixed up most often, for the statistics screen. */
    fun confusionPairs(limit: Int = 10): List<ConfusionPair> {
        val rows = db.rawQuery(
            """
            SELECT MIN(noteId, otherNoteId) AS a, MAX(noteId, otherNoteId) AS b, COUNT(*) AS n
            FROM confusions GROUP BY a, b ORDER BY n DESC LIMIT $limit
            """.trimIndent(),
            null,
        ).mapAll { Triple(it.getLong(0), it.getLong(1), it.getInt(2)) }
        val involved = notes(rows.flatMap { listOf(it.first, it.second) })
        return rows.mapNotNull { (a, b, n) ->
            val first = involved[a] ?: return@mapNotNull null
            val second = involved[b] ?: return@mapNotNull null
            ConfusionPair(first, second, n, areLinked(a, b))
        }
    }

    fun transaction(body: () -> Unit) {
        db.beginTransaction()
        try { body(); db.setTransactionSuccessful() } finally { db.endTransaction() }
    }

    internal fun raw(): SQLiteDatabase = db

    companion object {
        const val KEY_RETENTION = "desired_retention"
        const val KEY_SHOW_RELATED = "show_related"
        const val KEY_MAX_REVIEWS = "max_reviews"
        const val KEY_SEEDED = "seeded"
        const val KEY_EXAM_DATE = "exam_date"
        const val KEY_SHORTLIST = "shortlist"
        const val KEY_SEED_PACKS = "seed_packs"
        const val KEY_REMINDER = "reminder_enabled"
        const val KEY_REMINDER_HOUR = "reminder_hour"
        const val KEY_REMINDER_MINUTE = "reminder_minute"

        /** The note types that pose a problem to work through, not a fact to recall. */
        val PROBLEM_TYPES = listOf(
            NoteType.MATH, NoteType.CHEM_CALC, NoteType.EISAKUBUN, NoteType.WAYAKU,
        )

        /** How close the target is pushed as the exam arrives. */
        const val EXAM_PEAK_RETENTION = 0.97

        /** Days before the exam over which the target ramps up. */
        const val RAMP_DAYS = 60

        /**
         * Wrong answers before a card counts as one you keep stumbling over.
         *
         * Low enough to catch it while it still matters, high enough that an ordinary
         * difficult word does not qualify.
         */
        const val LEECH_MISSES = 5
    }
}
