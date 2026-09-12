package com.tango.recall

import androidx.test.core.app.ApplicationProvider
import com.tango.recall.data.Card
import com.tango.recall.data.Deck
import com.tango.recall.data.Note
import com.tango.recall.data.NoteType
import com.tango.recall.data.Repository
import com.tango.recall.data.TangoDb
import com.tango.recall.srs.CardPhase
import com.tango.recall.srs.Rating
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Taking back the last grade.
 *
 * FSRS cannot be told "that answer was not true" — stability and difficulty are
 * rewritten in place — so undo restores the card wholesale and removes the review it
 * logged. A mis-tapped button used to be permanent.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class UndoTest {

    private lateinit var repo: Repository
    private lateinit var card: Card

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        context.deleteDatabase(TangoDb.DB_NAME)
        repo = Repository(TangoDb(context))
        val deckId = repo.saveDeck(
            Deck(name = "英単語", noteTypeId = NoteType.ENGLISH.id, enabledTemplates = setOf("en_ja"))
        )
        val noteId = repo.saveNote(
            Note(
                deckId = deckId,
                typeId = NoteType.ENGLISH.id,
                fields = mapOf("word" to "abandon", "meaning" to "〜を見捨てる"),
            )
        )
        card = repo.cardsOfNote(noteId).single()
    }

    private fun reviewCount() =
        repo.raw().rawQuery("SELECT COUNT(*) FROM reviews", null)
            .use { if (it.moveToFirst()) it.getInt(0) else 0 }

    @Test
    fun undoPutsTheCardBackExactlyAsItWas() {
        val before = card
        repo.answer(before, Rating.EASY)
        assertNotEquals(before.srs.stability, repo.card(before.id)!!.srs.stability)

        repo.undoAnswer(before)

        val after = repo.card(before.id)!!
        assertEquals(before.srs.stability, after.srs.stability, 1e-12)
        assertEquals(before.srs.difficulty, after.srs.difficulty, 1e-12)
        assertEquals(before.srs.due, after.srs.due)
        assertEquals(before.srs.reps, after.srs.reps)
        assertEquals(before.srs.lapses, after.srs.lapses)
        assertEquals(CardPhase.NEW, after.srs.phase)
        assertEquals(before.srs.lastReview, after.srs.lastReview)
    }

    @Test
    fun undoTakesTheReviewOutOfTheLog() {
        repo.answer(card, Rating.GOOD)
        assertEquals(1, reviewCount())

        repo.undoAnswer(card)

        assertEquals(0, reviewCount())
        assertEquals("the day's tally must not count a taken-back answer", 0, repo.stats().reviewsToday)
    }

    @Test
    fun undoRemovesOnlyTheLastReview() {
        val first = repo.answer(card, Rating.GOOD)
        repo.answer(first, Rating.AGAIN)
        assertEquals(2, reviewCount())

        repo.undoAnswer(first)

        assertEquals(1, reviewCount())
        val restored = repo.card(card.id)!!
        assertEquals(first.srs.stability, restored.srs.stability, 1e-12)
        assertEquals(first.srs.phase, restored.srs.phase)
    }

    @Test
    fun aNewCardComesBackAsNewAndIsOfferedAgain() {
        val deckId = repo.listDecks().single().id
        repo.answer(card, Rating.GOOD)

        repo.undoAnswer(card)

        assertEquals("it counts as untouched again", 1, repo.deckCounts().getValue(deckId).newCount)
        assertEquals(listOf(card.id), repo.buildQueue(deckId))
    }
}
