package com.tango.recall.data

import com.tango.recall.srs.CardPhase
import com.tango.recall.srs.SrsState
import org.json.JSONArray
import org.json.JSONObject

data class ImportResult(val added: Int, val updated: Int, val skipped: Int, val message: String)

/**
 * Backup / restore and plain-text import.
 *
 * The JSON backup is complete (including scheduling state), so it round-trips a whole
 * install. TSV import is the fast path for typing a word list on a computer.
 */
object ImportExport {

    private const val FORMAT_VERSION = 1

    // ---- JSON backup --------------------------------------------------------

    fun exportJson(repo: Repository): String {
        val root = JSONObject()
        root.put("format", "tango-backup")
        root.put("version", FORMAT_VERSION)
        root.put("exportedAt", System.currentTimeMillis())

        val decks = JSONArray()
        val notesArr = JSONArray()
        val cardsArr = JSONArray()

        for (deck in repo.listDecks()) {
            decks.put(
                JSONObject()
                    .put("id", deck.id)
                    .put("name", deck.name)
                    .put("noteType", deck.noteTypeId)
                    .put("enabledTemplates", JSONArray(deck.enabledTemplates.toList()))
                    .put("newPerDay", deck.newPerDay)
                    .put("created", deck.created)
            )
            for (note in repo.listNotes(deck.id, "", Int.MAX_VALUE)) {
                notesArr.put(
                    JSONObject()
                        .put("id", note.id)
                        .put("deckId", note.deckId)
                        .put("type", note.typeId)
                        .put("fields", JSONObject(note.fields as Map<*, *>))
                        .put("tags", JSONArray(note.tags))
                        .put("created", note.created)
                        .put("modified", note.modified)
                )
                for (card in repo.cardsOfNote(note.id)) {
                    cardsArr.put(
                        JSONObject()
                            .put("noteId", card.noteId)
                            .put("templateId", card.templateId)
                            .put("stability", card.srs.stability)
                            .put("difficulty", card.srs.difficulty)
                            .put("due", card.srs.due)
                            .put("lastReview", card.srs.lastReview ?: JSONObject.NULL)
                            .put("phase", card.srs.phase.name)
                            .put("step", card.srs.step)
                            .put("reps", card.srs.reps)
                            .put("lapses", card.srs.lapses)
                            .put("suspended", card.suspended)
                    )
                }
            }
        }

        val linksArr = JSONArray()
        repo.raw().rawQuery("SELECT * FROM links", null).mapAll { it.toLink() }.forEach { l ->
            linksArr.put(
                JSONObject()
                    .put("from", l.fromNoteId).put("to", l.toNoteId)
                    .put("type", l.typeId).put("memo", l.memo)
            )
        }

        val settings = JSONObject()
            .put(Repository.KEY_RETENTION, repo.desiredRetention)
            .put(Repository.KEY_SHOW_RELATED, repo.showRelated)
            .put(Repository.KEY_MAX_REVIEWS, repo.maxReviewsPerDay)

        root.put("decks", decks)
        root.put("notes", notesArr)
        root.put("cards", cardsArr)
        root.put("links", linksArr)
        root.put("settings", settings)
        return root.toString(2)
    }

    /** Restore a backup. Existing content is replaced entirely. */
    fun importJson(repo: Repository, text: String): ImportResult {
        val root = try {
            JSONObject(text)
        } catch (e: Exception) {
            return ImportResult(0, 0, 0, "JSON として読めませんでした: ${e.message}")
        }
        if (root.optString("format") != "tango-backup") {
            return ImportResult(0, 0, 0, "Tango のバックアップファイルではありません")
        }

        var notes = 0
        var links = 0
        repo.transaction {
            val db = repo.raw()
            db.execSQL("DELETE FROM links")
            db.execSQL("DELETE FROM cards")
            db.execSQL("DELETE FROM notes")
            db.execSQL("DELETE FROM decks")

            val deckIdMap = HashMap<Long, Long>()
            val decks = root.optJSONArray("decks") ?: JSONArray()
            for (i in 0 until decks.length()) {
                val d = decks.getJSONObject(i)
                val templates = d.optJSONArray("enabledTemplates") ?: JSONArray()
                val newId = repo.saveDeck(
                    Deck(
                        name = d.optString("name", "デッキ"),
                        noteTypeId = d.optString("noteType", NoteType.BASIC.id),
                        enabledTemplates = buildSet {
                            for (j in 0 until templates.length()) add(templates.getString(j))
                        },
                        newPerDay = d.optInt("newPerDay", 20),
                        created = d.optLong("created", System.currentTimeMillis()),
                    )
                )
                deckIdMap[d.optLong("id")] = newId
            }

            val noteIdMap = HashMap<Long, Long>()
            val notesArr = root.optJSONArray("notes") ?: JSONArray()
            for (i in 0 until notesArr.length()) {
                val n = notesArr.getJSONObject(i)
                val deckId = deckIdMap[n.optLong("deckId")] ?: continue
                val tagsArr = n.optJSONArray("tags") ?: JSONArray()
                val newId = repo.saveNote(
                    Note(
                        deckId = deckId,
                        typeId = n.optString("type", NoteType.BASIC.id),
                        fields = n.optJSONObject("fields")?.toString()?.toFieldMap() ?: emptyMap(),
                        tags = buildList { for (j in 0 until tagsArr.length()) add(tagsArr.getString(j)) },
                        created = n.optLong("created", System.currentTimeMillis()),
                    )
                )
                noteIdMap[n.optLong("id")] = newId
                notes++
            }

            val cardsArr = root.optJSONArray("cards") ?: JSONArray()
            for (i in 0 until cardsArr.length()) {
                val c = cardsArr.getJSONObject(i)
                val noteId = noteIdMap[c.optLong("noteId")] ?: continue
                val existing = repo.cardsOfNote(noteId)
                    .firstOrNull { it.templateId == c.optString("templateId") } ?: continue
                val restored = existing.copy(
                    srs = SrsState(
                        stability = c.optDouble("stability", 0.0),
                        difficulty = c.optDouble("difficulty", 0.0),
                        due = c.optLong("due", 0L),
                        lastReview = if (c.isNull("lastReview")) null else c.optLong("lastReview"),
                        phase = CardPhase.fromName(c.optString("phase", "NEW")),
                        step = c.optInt("step", 0),
                        reps = c.optInt("reps", 0),
                        lapses = c.optInt("lapses", 0),
                    ),
                    suspended = c.optBoolean("suspended", false),
                )
                db.update("cards", restored.toValues(), "id=?", arrayOf(existing.id.toString()))
            }

            val linksArr = root.optJSONArray("links") ?: JSONArray()
            for (i in 0 until linksArr.length()) {
                val l = linksArr.getJSONObject(i)
                val from = noteIdMap[l.optLong("from")] ?: continue
                val to = noteIdMap[l.optLong("to")] ?: continue
                if (repo.addLink(from, to, LinkType.fromId(l.optString("type")), l.optString("memo"))) links++
            }

            root.optJSONObject("settings")?.let { s ->
                repo.desiredRetention = s.optDouble(Repository.KEY_RETENTION, 0.9)
                repo.showRelated = s.optBoolean(Repository.KEY_SHOW_RELATED, true)
                repo.maxReviewsPerDay = s.optInt(Repository.KEY_MAX_REVIEWS, 200)
            }
            repo.putSetting(Repository.KEY_SEEDED, "1")
        }
        return ImportResult(notes, 0, 0, "ノート ${notes} 件、関係 ${links} 件を復元しました")
    }

    // ---- TSV / CSV ----------------------------------------------------------

    /**
     * Import tab- or comma-separated rows into [deckId].
     *
     * If the first row names fields (by id or by Japanese label) it is used as a
     * header; otherwise columns are taken in the note type's field order.
     * A `tags` column is recognised, and a `#` column is ignored.
     */
    fun importDelimited(repo: Repository, deckId: Long, text: String): ImportResult {
        val deck = repo.deck(deckId) ?: return ImportResult(0, 0, 0, "デッキが見つかりません")
        val type = deck.noteType
        val lines = text.lines().map { it.trimEnd('\r') }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return ImportResult(0, 0, 0, "空のファイルです")

        val delimiter = if (lines.first().contains('\t')) '\t' else ','
        val rows = lines.map { splitRow(it, delimiter) }

        val headerIds = rows.first().map { cell ->
            val c = cell.trim()
            type.fields.firstOrNull { it.id.equals(c, true) || it.label == c }?.id
                ?: if (c.equals("tags", true) || c == "タグ") "tags" else null
        }
        val useHeader = headerIds.count { it != null } >= maxOf(2, headerIds.size - 1)
        val columns: List<String?> =
            if (useHeader) headerIds else type.fields.map { it.id }
        val body = if (useHeader) rows.drop(1) else rows

        var added = 0
        var skipped = 0
        val existing = repo.listNotes(deckId, "", Int.MAX_VALUE).associateBy { it.title() }
        var updated = 0

        repo.transaction {
            for (row in body) {
                val fields = mutableMapOf<String, String>()
                var tags = emptyList<String>()
                row.forEachIndexed { i, value ->
                    val col = columns.getOrNull(i) ?: return@forEachIndexed
                    val v = value.trim()
                    if (col == "tags") tags = v.split(" ", ",").filter { it.isNotBlank() }
                    else if (v.isNotEmpty()) fields[col] = v
                }
                if (fields.values.all { it.isBlank() }) { skipped++; continue }

                val candidate = Note(deckId = deckId, typeId = type.id, fields = fields, tags = tags)
                val title = candidate.title()
                val prior = existing[title]
                if (prior != null) {
                    // Merge: imported values win, existing extra fields are kept.
                    repo.saveNote(prior.copy(fields = prior.fields + fields, tags = (prior.tags + tags).distinct()))
                    updated++
                } else {
                    repo.saveNote(candidate)
                    added++
                }
            }
        }
        return ImportResult(added, updated, skipped, "追加 ${added} / 更新 ${updated} / スキップ ${skipped}")
    }

    /** Export one deck as TSV, ready to edit in a spreadsheet and re-import. */
    fun exportTsv(repo: Repository, deckId: Long): String {
        val deck = repo.deck(deckId) ?: return ""
        val type = deck.noteType
        val sb = StringBuilder()
        sb.append(type.fields.joinToString("\t") { it.id }).append("\ttags\n")
        for (note in repo.listNotes(deckId, "", Int.MAX_VALUE).sortedBy { it.created }) {
            sb.append(type.fields.joinToString("\t") { note[it.id].replace("\t", " ").replace("\n", "\\n") })
            sb.append('\t').append(note.tags.joinToString(" ")).append('\n')
        }
        return sb.toString()
    }

    private fun splitRow(line: String, delimiter: Char): List<String> {
        if (delimiter == '\t') return line.split('\t').map { it.replace("\\n", "\n") }
        // Minimal CSV: honour double quotes so commas inside a field survive.
        val out = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                ch == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> { sb.append('"'); i++ }
                ch == '"' -> inQuotes = !inQuotes
                ch == delimiter && !inQuotes -> { out += sb.toString(); sb.clear() }
                else -> sb.append(ch)
            }
            i++
        }
        out += sb.toString()
        return out.map { it.replace("\\n", "\n") }
    }
}
