package com.tango.recall

import androidx.test.core.app.ApplicationProvider
import com.tango.recall.data.Deck
import com.tango.recall.data.Note
import com.tango.recall.data.NoteType
import com.tango.recall.data.Repository
import com.tango.recall.data.TangoDb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Scheduling that aims at a date rather than at today. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ExamScheduleTest {

    private lateinit var repo: Repository
    private var deckId = 0L
    private val day = 86_400_000L
    private val now = 1_800_000_000_000L

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        context.deleteDatabase(TangoDb.DB_NAME)
        repo = Repository(TangoDb(context))
        deckId = repo.saveDeck(
            Deck(name = "英単語", noteTypeId = NoteType.ENGLISH.id, enabledTemplates = setOf("en_ja"))
        )
    }

    private fun studiedCard(word: String, stability: Double, reviewedDaysAgo: Int): Long {
        val noteId = repo.saveNote(
            Note(
                deckId = deckId,
                typeId = NoteType.ENGLISH.id,
                fields = mapOf("word" to word, "meaning" to "$word の意味"),
            )
        )
        val cardId = repo.cardsOfNote(noteId).single().id
        repo.raw().execSQL(
            "UPDATE cards SET phase='REVIEW', stability=?, difficulty=5.0, lastReview=?, due=?, reps=3 WHERE id=?",
            arrayOf(stability, now - reviewedDaysAgo * day, now + day, cardId),
        )
        return cardId
    }

    private fun newCard(word: String): Long {
        val noteId = repo.saveNote(
            Note(
                deckId = deckId,
                typeId = NoteType.ENGLISH.id,
                fields = mapOf("word" to word, "meaning" to "$word の意味"),
            )
        )
        return repo.cardsOfNote(noteId).single().id
    }

    @Test
    fun withNoExamSetNothingChanges() {
        assertNull(repo.daysUntilExam(now))
        assertNull(repo.examOutlook(now))
        assertEquals(repo.desiredRetention, repo.effectiveRetention(now), 1e-9)
        assertTrue(repo.buildExamQueue(now).isEmpty())
    }

    @Test
    fun theRetentionTargetTightensAsTheExamApproaches() {
        val base = repo.desiredRetention

        repo.examDate = now + 200 * day
        assertEquals("far out, nothing should change", base, repo.effectiveRetention(now), 1e-9)

        repo.examDate = now + 30 * day
        val halfway = repo.effectiveRetention(now)
        assertTrue("target should have risen", halfway > base)
        assertTrue(halfway < Repository.EXAM_PEAK_RETENTION)

        repo.examDate = now + 5 * day
        val close = repo.effectiveRetention(now)
        assertTrue("closer to the exam means a higher target", close > halfway)
        assertTrue(close <= Repository.EXAM_PEAK_RETENTION)
    }

    @Test
    fun theTargetNeverExceedsThePeakOrDropsBelowTheBaseline() {
        repo.examDate = now + day
        assertTrue(repo.effectiveRetention(now) <= Repository.EXAM_PEAK_RETENTION)
        // After the date has passed, ordinary scheduling resumes.
        repo.examDate = now - 10 * day
        assertEquals(repo.desiredRetention, repo.effectiveRetention(now), 1e-9)
    }

    @Test
    fun aShorterTargetProducesShorterIntervals() {
        repo.desiredRetention = 0.9
        val relaxed = repo.scheduler(now).desiredRetention
        repo.examDate = now + 3 * day
        val urgent = repo.scheduler(now).desiredRetention
        assertTrue("the scheduler must actually use the ramped target", urgent > relaxed)
    }

    @Test
    fun theOutlookReportsWhatWillHaveDecayedByTheDay() {
        // Solid: reviewed recently with a long stability. Fading: stability far shorter
        // than the time remaining.
        studiedCard("solid", stability = 400.0, reviewedDaysAgo = 1)
        studiedCard("fading", stability = 3.0, reviewedDaysAgo = 1)
        newCard("untouched")

        repo.examDate = now + 40 * day
        val outlook = repo.examOutlook(now)!!

        assertEquals(40, outlook.daysLeft)
        assertEquals(2, outlook.studiedCards)
        assertEquals(1, outlook.untouchedCards)
        assertEquals("only the fading card is below target on the day", 1, outlook.atRisk)
        val (weakestNote, weakestRecall) = outlook.weakest.first()
        assertEquals("fading", weakestNote.title())
        // FSRS decays as a power law, so even a badly faded card sits well above zero;
        // what matters is that it is below the target and behind the solid one.
        assertTrue("predicted $weakestRecall should be below the target", weakestRecall < repo.desiredRetention)
        assertTrue(weakestRecall < outlook.weakest.last().second)
    }

    @Test
    fun theQueueIsOrderedByWhatWillBeWeakestOnTheDay() {
        studiedCard("worst", stability = 2.0, reviewedDaysAgo = 1)
        studiedCard("middling", stability = 8.0, reviewedDaysAgo = 1)
        studiedCard("safe", stability = 500.0, reviewedDaysAgo = 1)
        newCard("untouched")

        repo.examDate = now + 30 * day
        val queue = repo.buildExamQueue(now)
        val words = queue.mapNotNull { id -> repo.card(id)?.let { repo.note(it.noteId)?.title() } }

        assertEquals(listOf("worst", "middling"), words)
    }

    @Test
    fun newCardsStayOutOfTheExamQueue() {
        newCard("untouched")
        repo.examDate = now + 30 * day
        assertTrue(
            "new cards are governed by the daily limit, not the countdown",
            repo.buildExamQueue(now).isEmpty(),
        )
    }

    @Test
    fun aCollectionThatIsAlreadySolidProducesAnEmptyQueue() {
        studiedCard("solid", stability = 1000.0, reviewedDaysAgo = 1)
        repo.examDate = now + 20 * day
        assertTrue(repo.buildExamQueue(now).isEmpty())
        assertEquals(0, repo.examOutlook(now)!!.atRisk)
    }

    @Test
    fun theExamDateSurvivesReopeningTheDatabase() {
        repo.examDate = now + 12 * day
        val reopened = Repository(TangoDb(ApplicationProvider.getApplicationContext()))
        assertEquals(now + 12 * day, reopened.examDate)
        assertEquals(12, reopened.daysUntilExam(now))
    }
}
