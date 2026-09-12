package com.tango.recall.data

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.tango.recall.srs.CardPhase
import com.tango.recall.srs.FsrsScheduler
import com.tango.recall.srs.Rating
import java.util.Calendar

data class RelationGroup(
    val type: LinkType,
    val reverse: Boolean,
    val partners: List<RelatedNote>,
)

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
)

data class GraphEdge(val from: Long, val to: Long, val typeId: String, val label: String)

data class GraphData(val nodes: List<GraphNode>, val edges: List<GraphEdge>)

data class DeckCounts(val newCount: Int, val learnCount: Int, val dueCount: Int, val total: Int) {
    val studyable: Int get() = newCount + learnCount + dueCount
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
    val hardest: List<Pair<Note, Int>>,
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

    fun scheduler(): FsrsScheduler = FsrsScheduler(desiredRetention = desiredRetention)

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
            listNotes(deck.id, "", Int.MAX_VALUE).forEach { regenerateCards(it, deck) }
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

    fun listNotes(deckId: Long?, query: String = "", limit: Int = 500): List<Note> {
        val args = mutableListOf<String>()
        val where = buildString {
            append("1=1")
            if (deckId != null) { append(" AND deckId=?"); args += deckId.toString() }
            if (query.isNotBlank()) {
                append(" AND (fields LIKE ? OR tags LIKE ?)")
                args += "%$query%"; args += "%$query%"
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
        for (card in existing.filter { it.templateId !in wanted }) {
            db.delete("cards", "id=?", arrayOf(card.id.toString()))
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

    fun card(id: Long): Card? =
        db.rawQuery("SELECT * FROM cards WHERE id=?", arrayOf(id.toString())).mapAll { it.toCard() }
            .firstOrNull()

    fun setSuspended(cardId: Long, suspended: Boolean) {
        db.execSQL("UPDATE cards SET suspended=? WHERE id=?", arrayOf(if (suspended) 1 else 0, cardId))
    }

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
        val newState = scheduler().review(card.srs, rating, now)
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

        var streak = 0
        for (d in 0..365) {
            val from = dayStart(now, d)
            val to = dayStart(now, d - 1)
            val n = scalarInt(
                "SELECT COUNT(*) FROM reviews WHERE ts>=? AND ts<?",
                arrayOf(from.toString(), to.toString()),
            )
            if (n > 0) streak++ else if (d > 0) break
        }

        val avgStability = db.rawQuery(
            "SELECT AVG(stability) FROM cards WHERE phase='REVIEW'", null,
        ).use { if (it.moveToFirst() && !it.isNull(0)) it.getDouble(0) else 0.0 }

        val hardestIds = db.rawQuery(
            "SELECT noteId, SUM(lapses) AS l FROM cards GROUP BY noteId HAVING l>0 ORDER BY l DESC LIMIT 10",
            null,
        ).mapAll { it.getLong(0) to it.getInt(1) }
        val hardestNotes = notes(hardestIds.map { it.first })
        val hardest = hardestIds.mapNotNull { (id, l) -> hardestNotes[id]?.let { it to l } }

        return Stats(
            totalNotes = scalarInt("SELECT COUNT(*) FROM notes"),
            totalCards = scalarInt("SELECT COUNT(*) FROM cards"),
            reviewsToday = reviewsToday,
            correctToday = correctToday,
            streakDays = streak,
            dueNext7Days = forecast,
            matureCards = scalarInt("SELECT COUNT(*) FROM cards WHERE phase='REVIEW' AND stability>=21"),
            averageStability = avgStability,
            hardest = hardest,
        )
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
    fun graph(deckId: Long?, now: Long = System.currentTimeMillis(), limit: Int = 500): GraphData {
        val notes = listNotes(deckId, "", limit)
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

        val nodes = notes.map { note ->
            val cards = cardsOfNote(note.id)
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
            )
        }

        val edges = allLinks.map {
            GraphEdge(it.fromNoteId, it.toNoteId, it.typeId, it.type.forward)
        }
        return GraphData(nodes, edges)
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
    }
}
