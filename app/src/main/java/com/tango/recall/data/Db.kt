package com.tango.recall.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.tango.recall.srs.CardPhase
import com.tango.recall.srs.SrsState
import org.json.JSONArray
import org.json.JSONObject

/**
 * Plain SQLite storage.
 *
 * Deliberately hand-rolled rather than Room: it keeps the build free of annotation
 * processors, and the schema is small enough that explicit SQL stays readable.
 */
class TangoDb(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE decks(
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              name TEXT NOT NULL,
              noteType TEXT NOT NULL,
              enabledTemplates TEXT NOT NULL,
              newPerDay INTEGER NOT NULL DEFAULT 20,
              relationQuiz INTEGER NOT NULL DEFAULT 0,
              created INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE notes(
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              deckId INTEGER NOT NULL REFERENCES decks(id) ON DELETE CASCADE,
              type TEXT NOT NULL,
              fields TEXT NOT NULL,
              tags TEXT NOT NULL DEFAULT '',
              created INTEGER NOT NULL,
              modified INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE cards(
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              noteId INTEGER NOT NULL REFERENCES notes(id) ON DELETE CASCADE,
              deckId INTEGER NOT NULL,
              templateId TEXT NOT NULL,
              stability REAL NOT NULL DEFAULT 0,
              difficulty REAL NOT NULL DEFAULT 0,
              due INTEGER NOT NULL DEFAULT 0,
              lastReview INTEGER,
              phase TEXT NOT NULL DEFAULT 'NEW',
              step INTEGER NOT NULL DEFAULT 0,
              reps INTEGER NOT NULL DEFAULT 0,
              lapses INTEGER NOT NULL DEFAULT 0,
              suspended INTEGER NOT NULL DEFAULT 0,
              UNIQUE(noteId, templateId)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE links(
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              fromNoteId INTEGER NOT NULL REFERENCES notes(id) ON DELETE CASCADE,
              toNoteId INTEGER NOT NULL REFERENCES notes(id) ON DELETE CASCADE,
              type TEXT NOT NULL,
              memo TEXT NOT NULL DEFAULT '',
              created INTEGER NOT NULL,
              UNIQUE(fromNoteId, toNoteId, type)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE reviews(
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              cardId INTEGER NOT NULL,
              noteId INTEGER NOT NULL,
              deckId INTEGER NOT NULL,
              rating INTEGER NOT NULL,
              ts INTEGER NOT NULL,
              phase TEXT NOT NULL,
              stability REAL NOT NULL,
              difficulty REAL NOT NULL,
              tookMs INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL(CREATE_CONFUSIONS)
        db.execSQL("CREATE TABLE settings(k TEXT PRIMARY KEY, v TEXT NOT NULL)")

        db.execSQL("CREATE INDEX idx_cards_due ON cards(deckId, suspended, due)")
        db.execSQL("CREATE INDEX idx_cards_note ON cards(noteId)")
        db.execSQL("CREATE INDEX idx_notes_deck ON notes(deckId)")
        db.execSQL("CREATE INDEX idx_links_from ON links(fromNoteId)")
        db.execSQL("CREATE INDEX idx_links_to ON links(toNoteId)")
        db.execSQL("CREATE INDEX idx_reviews_ts ON reviews(ts)")
        db.execSQL(INDEX_CONFUSIONS)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Migrations are additive and must never drop a learner's review history.
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE decks ADD COLUMN relationQuiz INTEGER NOT NULL DEFAULT 0")
        }
        if (oldVersion < 3) {
            db.execSQL(CREATE_CONFUSIONS)
            db.execSQL(INDEX_CONFUSIONS)
        }
    }

    companion object {
        const val DB_NAME = "tango.db"
        const val DB_VERSION = 3

        /** Every time one note's answer was written where another note's was wanted. */
        private const val CREATE_CONFUSIONS = """
            CREATE TABLE confusions(
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              noteId INTEGER NOT NULL REFERENCES notes(id) ON DELETE CASCADE,
              otherNoteId INTEGER NOT NULL REFERENCES notes(id) ON DELETE CASCADE,
              templateId TEXT NOT NULL,
              typed TEXT NOT NULL,
              ts INTEGER NOT NULL
            )
        """

        private const val INDEX_CONFUSIONS =
            "CREATE INDEX idx_confusions_pair ON confusions(noteId, otherNoteId)"
    }
}

// ---- cursor helpers ---------------------------------------------------------

internal fun Cursor.str(name: String): String = getString(getColumnIndexOrThrow(name)) ?: ""
internal fun Cursor.strOrNull(name: String): String? {
    val i = getColumnIndexOrThrow(name)
    return if (isNull(i)) null else getString(i)
}

internal fun Cursor.long(name: String): Long = getLong(getColumnIndexOrThrow(name))
internal fun Cursor.longOrNull(name: String): Long? {
    val i = getColumnIndexOrThrow(name)
    return if (isNull(i)) null else getLong(i)
}

internal fun Cursor.int(name: String): Int = getInt(getColumnIndexOrThrow(name))
internal fun Cursor.dbl(name: String): Double = getDouble(getColumnIndexOrThrow(name))

internal inline fun <T> Cursor.mapAll(body: (Cursor) -> T): List<T> = use { c ->
    buildList { while (c.moveToNext()) add(body(c)) }
}

// ---- (de)serialisation ------------------------------------------------------

internal fun Map<String, String>.toJson(): String = JSONObject(this as Map<*, *>).toString()

internal fun String.toFieldMap(): Map<String, String> = try {
    val o = JSONObject(this)
    buildMap { o.keys().forEach { k -> put(k, o.optString(k, "")) } }
} catch (_: Exception) {
    emptyMap()
}

internal fun List<String>.tagsToDb(): String =
    filter { it.isNotBlank() }.joinToString(" ") { it.trim() }

internal fun String.tagsFromDb(): List<String> =
    split(" ", ",").map { it.trim() }.filter { it.isNotEmpty() }

internal fun Set<String>.toJsonArray(): String = JSONArray(this.toList()).toString()

internal fun String.toStringSet(): Set<String> = try {
    val a = JSONArray(this)
    buildSet { for (i in 0 until a.length()) add(a.getString(i)) }
} catch (_: Exception) {
    emptySet()
}

internal fun Cursor.toDeck() = Deck(
    id = long("id"),
    name = str("name"),
    noteTypeId = str("noteType"),
    enabledTemplates = str("enabledTemplates").toStringSet(),
    newPerDay = int("newPerDay"),
    relationQuiz = int("relationQuiz") != 0,
    created = long("created"),
)

internal fun Cursor.toNote() = Note(
    id = long("id"),
    deckId = long("deckId"),
    typeId = str("type"),
    fields = str("fields").toFieldMap(),
    tags = str("tags").tagsFromDb(),
    created = long("created"),
    modified = long("modified"),
)

internal fun Cursor.toCard() = Card(
    id = long("id"),
    noteId = long("noteId"),
    deckId = long("deckId"),
    templateId = str("templateId"),
    srs = SrsState(
        stability = dbl("stability"),
        difficulty = dbl("difficulty"),
        due = long("due"),
        lastReview = longOrNull("lastReview"),
        phase = CardPhase.fromName(str("phase")),
        step = int("step"),
        reps = int("reps"),
        lapses = int("lapses"),
    ),
    suspended = int("suspended") != 0,
)

internal fun Cursor.toLink() = NoteLink(
    id = long("id"),
    fromNoteId = long("fromNoteId"),
    toNoteId = long("toNoteId"),
    typeId = str("type"),
    memo = str("memo"),
)

internal fun Card.toValues(): ContentValues = ContentValues().apply {
    put("noteId", noteId)
    put("deckId", deckId)
    put("templateId", templateId)
    put("stability", srs.stability)
    put("difficulty", srs.difficulty)
    put("due", srs.due)
    if (srs.lastReview == null) putNull("lastReview") else put("lastReview", srs.lastReview)
    put("phase", srs.phase.name)
    put("step", srs.step)
    put("reps", srs.reps)
    put("lapses", srs.lapses)
    put("suspended", if (suspended) 1 else 0)
}
