package com.tango.recall

import androidx.test.core.app.ApplicationProvider
import com.tango.recall.data.Card
import com.tango.recall.data.Deck
import com.tango.recall.data.Note
import com.tango.recall.data.NoteType
import com.tango.recall.data.Repository
import com.tango.recall.data.TangoDb
import com.tango.recall.data.cardLabel
import com.tango.recall.srs.Rating
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Finding the cards that keep coming back wrong.
 *
 * Counted from the review log, not from the lapse counter: a card can be failed over
 * and over while still in learning without the lapse counter ever moving, and those
 * are exactly the ones worth rewriting.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LeechTest {

    private lateinit var repo: Repository
    private var deckId = 0L

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        context.deleteDatabase(TangoDb.DB_NAME)
        repo = Repository(TangoDb(context))
        deckId = repo.saveDeck(
            Deck(
                name = "英単語",
                noteTypeId = NoteType.ENGLISH.id,
                enabledTemplates = setOf("en_ja", "ja_en"),
            )
        )
    }

    private fun word(word: String): Long = repo.saveNote(
        Note(
            deckId = deckId,
            typeId = NoteType.ENGLISH.id,
            fields = mapOf("word" to word, "meaning" to "意味：$word"),
        )
    )

    private fun card(noteId: Long, templateId: String): Card =
        repo.cardsOfNote(noteId).single { it.templateId == templateId }

    private fun miss(cardId: Long, times: Int) {
        repeat(times) { repo.answer(repo.card(cardId)!!, Rating.AGAIN) }
    }

    @Test
    fun aCardMissedOverAndOverIsPickedUp() {
        val noteId = word("abandon")
        val id = card(noteId, "ja_en").id
        miss(id, Repository.LEECH_MISSES)

        val leech = repo.leeches().single()
        assertEquals(id, leech.card.id)
        assertEquals(Repository.LEECH_MISSES, leech.misses)
        assertEquals("和 → 英（入力）", leech.label)
    }

    @Test
    fun anOrdinaryDifficultCardIsLeftAlone() {
        val noteId = word("abandon")
        miss(card(noteId, "ja_en").id, Repository.LEECH_MISSES - 1)
        assertTrue(repo.leeches().isEmpty())
    }

    @Test
    fun onlyTheDirectionThatFailsIsNamed() {
        val noteId = word("abandon")
        val typed = card(noteId, "ja_en").id
        miss(typed, Repository.LEECH_MISSES)
        repo.answer(repo.card(card(noteId, "en_ja").id)!!, Rating.GOOD)

        val leeches = repo.leeches()
        assertEquals("recognising it is fine; producing it is not", 1, leeches.size)
        assertEquals(typed, leeches.single().card.id)
    }

    @Test
    fun worstFirst() {
        val a = card(word("abandon"), "ja_en").id
        val b = card(word("conspicuous"), "ja_en").id
        miss(a, Repository.LEECH_MISSES)
        miss(b, Repository.LEECH_MISSES + 3)

        assertEquals(listOf(b, a), repo.leeches().map { it.card.id })
    }

    @Test
    fun aRelationQuestionIsNamedByItsRelation() {
        val note = repo.note(word("abandon"))!!
        assertEquals("つながり: 同語根", cardLabel(note, "rel:same_root"))
        assertEquals("つながり: から作られる", cardLabel(note, "rel:produces:r"))
    }
}
