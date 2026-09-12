package com.tango.recall

import androidx.test.core.app.ApplicationProvider
import com.tango.recall.data.Card
import com.tango.recall.data.Deck
import com.tango.recall.data.Note
import com.tango.recall.data.NoteType
import com.tango.recall.data.Repository
import com.tango.recall.data.TangoDb
import com.tango.recall.srs.Rating
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * What happens to a card when the direction it asks in stops applying.
 *
 * Deleting it was taking weeks of review history with it — switching a direction off
 * by mistake, or clearing a field to retype it, was unrecoverable. A card that has
 * been studied is put away instead, and comes back with its schedule intact.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CardLifecycleTest {

    private lateinit var repo: Repository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        context.deleteDatabase(TangoDb.DB_NAME)
        repo = Repository(TangoDb(context))
    }

    private val allDirections = setOf("en_ja", "ja_en", "cloze")

    private fun deck(): Long = repo.saveDeck(
        Deck(name = "英単語", noteTypeId = NoteType.ENGLISH.id, enabledTemplates = allDirections)
    )

    private fun note(deckId: Long) = Note(
        deckId = deckId,
        typeId = NoteType.ENGLISH.id,
        fields = mapOf(
            "word" to "abandon",
            "meaning" to "〜を見捨てる",
            "example" to "He abandoned his plan.",
            "exampleJa" to "彼は計画を放棄した。",
        ),
    )

    private fun cloze(noteId: Long): Card = repo.cardsOfNote(noteId).single { it.templateId == "cloze" }

    /** Give a card a history worth protecting. */
    private fun study(noteId: Long): Card {
        repo.answer(cloze(noteId), Rating.GOOD)
        repo.answer(cloze(noteId), Rating.GOOD, System.currentTimeMillis() + 86_400_000L)
        return cloze(noteId)
    }

    private fun setDirections(deckId: Long, templates: Set<String>) {
        repo.saveDeck(repo.deck(deckId)!!.copy(enabledTemplates = templates))
    }

    @Test
    fun turningADirectionOffKeepsAStudiedCardAndItsSchedule() {
        val deckId = deck()
        val noteId = repo.saveNote(note(deckId))
        val before = study(noteId)

        setDirections(deckId, setOf("en_ja", "ja_en"))

        val after = cloze(noteId)
        assertTrue("the card must survive", after.suspended)
        assertTrue("and be marked as put away by the app", after.autoSuspended)
        assertEquals(before.srs.reps, after.srs.reps)
        assertEquals(before.srs.stability, after.srs.stability, 1e-9)
        assertFalse("a put-away card must not be asked", after.id in repo.buildQueue(deckId))
    }

    @Test
    fun turningItBackOnRestoresTheCardWhereItLeftOff() {
        val deckId = deck()
        val noteId = repo.saveNote(note(deckId))
        val before = study(noteId)

        setDirections(deckId, setOf("en_ja", "ja_en"))
        setDirections(deckId, allDirections)

        val after = cloze(noteId)
        assertFalse(after.suspended)
        assertFalse(after.autoSuspended)
        assertEquals(before.srs.reps, after.srs.reps)
        assertEquals(before.srs.stability, after.srs.stability, 1e-9)
        assertEquals(before.srs.due, after.srs.due)
    }

    @Test
    fun clearingTheFieldACardAsksAboutPutsItAwayRatherThanDeletingIt() {
        val deckId = deck()
        val noteId = repo.saveNote(note(deckId))
        val before = study(noteId)

        val saved = repo.note(noteId)!!
        repo.saveNote(saved.copy(fields = saved.fields + ("example" to "")))
        assertTrue(cloze(noteId).suspended)

        repo.saveNote(repo.note(noteId)!!.copy(fields = saved.fields))
        val after = cloze(noteId)
        assertFalse("retyping the sentence must bring the card back", after.suspended)
        assertEquals(before.srs.reps, after.srs.reps)
    }

    @Test
    fun aCardNeverStudiedIsStillDropped() {
        val deckId = deck()
        val noteId = repo.saveNote(note(deckId))

        setDirections(deckId, setOf("en_ja"))

        assertEquals(
            "there is no history to protect, so it goes",
            listOf("en_ja"),
            repo.cardsOfNote(noteId).map { it.templateId },
        )
    }

    @Test
    fun aCardSuspendedByHandStaysSuspended() {
        val deckId = deck()
        val noteId = repo.saveNote(note(deckId))
        val card = study(noteId)
        repo.setSuspended(card.id, true)

        setDirections(deckId, setOf("en_ja", "ja_en"))
        setDirections(deckId, allDirections)

        assertTrue("the learner's own choice outranks the automatic one", cloze(noteId).suspended)
    }
}
