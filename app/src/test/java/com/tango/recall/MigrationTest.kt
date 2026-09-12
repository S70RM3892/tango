package com.tango.recall

import android.app.Application
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.tango.recall.data.Repository
import com.tango.recall.data.TangoDb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Upgrading an install that already holds review history.
 *
 * Version 1 shipped before relation cards existed, so a device in the field has the
 * old `decks` table. The upgrade must add the column without touching anything else.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MigrationTest {

    private lateinit var context: Application

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(TangoDb.DB_NAME)
    }

    private fun Repository(context: Application) = com.tango.recall.data.Repository(TangoDb(context))

    /** Recreate exactly the schema that version 1 wrote. */
    private fun createVersion1Database(): SQLiteDatabase {
        val path = context.getDatabasePath(TangoDb.DB_NAME)
        path.parentFile?.mkdirs()
        val db = SQLiteDatabase.openOrCreateDatabase(path, null)
        db.execSQL(
            "CREATE TABLE decks(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, " +
                "noteType TEXT NOT NULL, enabledTemplates TEXT NOT NULL, " +
                "newPerDay INTEGER NOT NULL DEFAULT 20, created INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE notes(id INTEGER PRIMARY KEY AUTOINCREMENT, deckId INTEGER NOT NULL " +
                "REFERENCES decks(id) ON DELETE CASCADE, type TEXT NOT NULL, fields TEXT NOT NULL, " +
                "tags TEXT NOT NULL DEFAULT '', created INTEGER NOT NULL, modified INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE cards(id INTEGER PRIMARY KEY AUTOINCREMENT, noteId INTEGER NOT NULL " +
                "REFERENCES notes(id) ON DELETE CASCADE, deckId INTEGER NOT NULL, templateId TEXT NOT NULL, " +
                "stability REAL NOT NULL DEFAULT 0, difficulty REAL NOT NULL DEFAULT 0, " +
                "due INTEGER NOT NULL DEFAULT 0, lastReview INTEGER, phase TEXT NOT NULL DEFAULT 'NEW', " +
                "step INTEGER NOT NULL DEFAULT 0, reps INTEGER NOT NULL DEFAULT 0, " +
                "lapses INTEGER NOT NULL DEFAULT 0, suspended INTEGER NOT NULL DEFAULT 0, " +
                "UNIQUE(noteId, templateId))"
        )
        db.execSQL(
            "CREATE TABLE links(id INTEGER PRIMARY KEY AUTOINCREMENT, fromNoteId INTEGER NOT NULL " +
                "REFERENCES notes(id) ON DELETE CASCADE, toNoteId INTEGER NOT NULL " +
                "REFERENCES notes(id) ON DELETE CASCADE, type TEXT NOT NULL, " +
                "memo TEXT NOT NULL DEFAULT '', created INTEGER NOT NULL, " +
                "UNIQUE(fromNoteId, toNoteId, type))"
        )
        db.execSQL(
            "CREATE TABLE reviews(id INTEGER PRIMARY KEY AUTOINCREMENT, cardId INTEGER NOT NULL, " +
                "noteId INTEGER NOT NULL, deckId INTEGER NOT NULL, rating INTEGER NOT NULL, " +
                "ts INTEGER NOT NULL, phase TEXT NOT NULL, stability REAL NOT NULL, " +
                "difficulty REAL NOT NULL, tookMs INTEGER NOT NULL DEFAULT 0)"
        )
        db.execSQL("CREATE TABLE settings(k TEXT PRIMARY KEY, v TEXT NOT NULL)")
        db.version = 1
        return db
    }

    @Test
    fun upgradingKeepsDecksNotesCardsAndReviewHistory() {
        val old = createVersion1Database()
        old.execSQL(
            "INSERT INTO decks(name, noteType, enabledTemplates, newPerDay, created) " +
                "VALUES('英単語', 'english', '[\"en_ja\",\"ja_en\"]', 15, 1000)"
        )
        old.execSQL(
            "INSERT INTO notes(deckId, type, fields, tags, created, modified) " +
                "VALUES(1, 'english', '{\"word\":\"abandon\",\"meaning\":\"見捨てる\"}', '語源', 1000, 1000)"
        )
        old.execSQL(
            "INSERT INTO cards(noteId, deckId, templateId, stability, difficulty, due, lastReview, " +
                "phase, step, reps, lapses, suspended) " +
                "VALUES(1, 1, 'en_ja', 12.5, 4.2, 2000, 1500, 'REVIEW', 0, 7, 1, 0)"
        )
        old.execSQL(
            "INSERT INTO reviews(cardId, noteId, deckId, rating, ts, phase, stability, difficulty, tookMs) " +
                "VALUES(1, 1, 1, 3, 1500, 'REVIEW', 12.5, 4.2, 4200)"
        )
        old.close()

        val repo = Repository(context)

        val deck = repo.listDecks().single()
        assertEquals("英単語", deck.name)
        assertEquals(15, deck.newPerDay)
        assertFalse("existing decks must not silently gain relation cards", deck.relationQuiz)

        val note = repo.listNotes(deck.id, "").single()
        assertEquals("abandon", note.title())
        assertEquals(listOf("語源"), note.tags)

        val card = repo.cardsOfNote(note.id).single { it.templateId == "en_ja" }
        assertEquals(12.5, card.srs.stability, 1e-9)
        assertEquals(7, card.srs.reps)
        assertEquals(1, card.srs.lapses)
        assertEquals(1500L, card.srs.lastReview)

        assertEquals(1, repo.stats(1500).reviewsToday)
    }

    @Test
    fun theUpgradedDatabaseCanStillTakeNewWrites() {
        createVersion1Database().close()
        val repo = Repository(context)
        val deckId = repo.saveDeck(
            com.tango.recall.data.Deck(
                name = "新デッキ",
                noteTypeId = com.tango.recall.data.NoteType.CHEM_CALC.id,
                enabledTemplates = setOf("calc"),
                relationQuiz = true,
            )
        )
        assertTrue(repo.deck(deckId)!!.relationQuiz)
    }

    /** Build the schema version 2 shipped, i.e. what is on a device running v1.2. */
    private fun createVersion2Database(): SQLiteDatabase {
        val db = createVersion1Database()
        db.execSQL("ALTER TABLE decks ADD COLUMN relationQuiz INTEGER NOT NULL DEFAULT 0")
        db.version = 2
        return db
    }

    @Test
    fun upgradingFromTheReleasedVersionKeepsEverythingAndAddsConfusions() {
        val old = createVersion2Database()
        old.execSQL(
            "INSERT INTO decks(name, noteType, enabledTemplates, newPerDay, relationQuiz, created) " +
                "VALUES('英単語', 'english', '[\"en_ja\",\"ja_en\"]', 15, 1, 1000)"
        )
        old.execSQL(
            "INSERT INTO notes(deckId, type, fields, tags, created, modified) " +
                "VALUES(1, 'english', '{\"word\":\"precede\",\"meaning\":\"先行する\"}', '', 1000, 1000)"
        )
        old.execSQL(
            "INSERT INTO notes(deckId, type, fields, tags, created, modified) " +
                "VALUES(1, 'english', '{\"word\":\"concede\",\"meaning\":\"認める\"}', '', 1000, 1000)"
        )
        old.execSQL(
            "INSERT INTO cards(noteId, deckId, templateId, stability, difficulty, due, lastReview, " +
                "phase, step, reps, lapses, suspended) " +
                "VALUES(1, 1, 'ja_en', 33.0, 4.0, 2000, 1500, 'REVIEW', 0, 9, 2, 0)"
        )
        old.close()

        val repo = Repository(context)

        // Nothing from before is disturbed...
        val deck = repo.listDecks().single()
        assertTrue("the relation-quiz setting must survive", deck.relationQuiz)
        assertEquals(2, repo.listNotes(deck.id, "").size)
        val card = repo.cardsOfNote(repo.listNotes(deck.id, "").first { it.title() == "precede" }.id)
            .single { it.templateId == "ja_en" }
        assertEquals(33.0, card.srs.stability, 1e-9)
        assertEquals(9, card.srs.reps)

        // ...and the new table is there and usable.
        val precede = repo.listNotes(deck.id, "").single { it.title() == "precede" }
        val concede = repo.listNotes(deck.id, "").single { it.title() == "concede" }
        assertEquals(1, repo.recordConfusion(precede.id, concede.id, "ja_en", "concede"))
        assertEquals(1, repo.confusionPairs().size)
    }

    @Test
    fun upgradingFromVersionTwoDoesNotReapplyTheVersionTwoStep() {
        // Re-running the v1 migration on a v2 database would fail on a duplicate column.
        createVersion2Database().close()
        val repo = Repository(context)
        assertEquals(0, repo.listDecks().size)
        repo.examDate = 1_800_000_000_000L
        assertEquals(1_800_000_000_000L, Repository(context).examDate)
    }

    @Test
    fun aFreshInstallStartsAtTheCurrentVersion() {
        val repo = Repository(context)
        repo.listDecks()
        assertEquals(TangoDb.DB_VERSION, context.getDatabasePath(TangoDb.DB_NAME).let {
            SQLiteDatabase.openDatabase(it.path, null, SQLiteDatabase.OPEN_READONLY).use { db -> db.version }
        })
    }
}
