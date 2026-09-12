package com.tango.recall

import androidx.test.core.app.ApplicationProvider
import com.tango.recall.data.Deck
import com.tango.recall.data.Note
import com.tango.recall.data.NoteType
import com.tango.recall.data.Repository
import com.tango.recall.data.TangoDb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Searching the collection.
 *
 * A note's fields are stored as JSON, and searching that text directly matched the
 * field *names* as well as their values — "memo" found every note ever written — and
 * missed anything org.json escapes, which includes the slash in "mol/L".
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SearchTest {

    private lateinit var repo: Repository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        context.deleteDatabase(TangoDb.DB_NAME)
        repo = Repository(TangoDb(context))
    }

    private fun englishDeck(): Long = repo.saveDeck(
        Deck(name = "英単語", noteTypeId = NoteType.ENGLISH.id, enabledTemplates = setOf("en_ja"))
    )

    private fun word(deckId: Long, word: String, meaning: String, tags: List<String> = emptyList()) =
        repo.saveNote(
            Note(
                deckId = deckId,
                typeId = NoteType.ENGLISH.id,
                fields = mapOf("word" to word, "meaning" to meaning),
                tags = tags,
            )
        )

    @Test
    fun fieldNamesAreNotContent() {
        val deckId = englishDeck()
        word(deckId, "abandon", "〜を見捨てる")

        assertTrue("\"memo\" is the name of an empty field", repo.listNotes(null, "memo").isEmpty())
        assertTrue(repo.listNotes(null, "exampleJa").isEmpty())
        assertEquals(1, repo.listNotes(null, "abandon").size)
    }

    @Test
    fun aSlashInTheAnswerIsFindable() {
        val deckId = repo.saveDeck(
            Deck(name = "計算", noteTypeId = NoteType.CHEM_CALC.id, enabledTemplates = setOf("calc"))
        )
        repo.saveNote(
            Note(
                deckId = deckId,
                typeId = NoteType.CHEM_CALC.id,
                fields = mapOf(
                    "question" to "0.10 mol/L の酢酸水溶液の pH",
                    "answer" to "2.8",
                    "unit" to "mol/L",
                ),
            )
        )
        assertEquals(1, repo.listNotes(null, "mol/L").size)
    }

    @Test
    fun searchIgnoresCase() {
        val deckId = englishDeck()
        word(deckId, "Conspicuous", "目立つ")
        assertEquals(1, repo.listNotes(null, "conspicuous").size)
        assertEquals(1, repo.listNotes(null, "CONSPICUOUS").size)
    }

    @Test
    fun tagsAreSearchable() {
        val deckId = englishDeck()
        word(deckId, "precede", "先行する", tags = listOf("語源", "混同注意"))
        word(deckId, "abandon", "見捨てる")
        assertEquals(listOf("precede"), repo.listNotes(null, "混同注意").map { it.title() })
    }

    @Test
    fun wildcardsAreSearchedForLiterally() {
        val deckId = englishDeck()
        word(deckId, "yield", "収率は 50% だった")
        word(deckId, "abandon", "見捨てる")

        assertEquals(listOf("yield"), repo.listNotes(null, "50%").map { it.title() })
        assertTrue("_ must not stand for any character", repo.listNotes(null, "5_").isEmpty())
        assertTrue(repo.listNotes(null, "%").size == 1)
    }
}
