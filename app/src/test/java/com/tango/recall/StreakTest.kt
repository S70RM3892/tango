package com.tango.recall

import androidx.test.core.app.ApplicationProvider
import com.tango.recall.data.Deck
import com.tango.recall.data.Note
import com.tango.recall.data.NoteType
import com.tango.recall.data.Repository
import com.tango.recall.data.TangoDb
import com.tango.recall.srs.Rating
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The "連続日数" figure on the statistics screen.
 *
 * It used to be counted by asking the database about each of the last 365 days in
 * turn; it now walks back from the most recent review and stops at the first gap, so
 * the answers it gives have to be exactly the same.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class StreakTest {

    private lateinit var repo: Repository
    private var cardId = 0L

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
        cardId = repo.cardsOfNote(noteId).single().id
    }

    /** Midday [daysAgo] days back — comfortably inside that study day either side. */
    private fun noon(daysAgo: Int): Long = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -daysAgo)
        set(Calendar.HOUR_OF_DAY, 12)
        set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private val now: Long get() = noon(0) + 3_600_000L

    private fun studyOn(vararg daysAgo: Int) {
        daysAgo.sortedDescending().forEach { repo.answer(repo.card(cardId)!!, Rating.GOOD, noon(it)) }
    }

    @Test
    fun consecutiveDaysEndingTodayAreCounted() {
        studyOn(0, 1, 2)
        assertEquals(3, repo.studyStreak(now))
    }

    @Test
    fun aMissedDayEndsTheStreak() {
        studyOn(0, 1, 3, 4)
        assertEquals(2, repo.studyStreak(now))
    }

    @Test
    fun todayNotStudiedYetDoesNotBreakYesterdaysStreak() {
        studyOn(1, 2)
        assertEquals(2, repo.studyStreak(now))
    }

    @Test
    fun aGapOfTwoDaysLeavesNoStreak() {
        studyOn(2, 3)
        assertEquals(0, repo.studyStreak(now))
    }

    @Test
    fun severalSessionsInOneDayCountOnce() {
        studyOn(0, 0, 1)
        assertEquals(2, repo.studyStreak(now))
    }

    @Test
    fun nothingStudiedIsNoStreak() {
        assertEquals(0, repo.studyStreak(now))
    }

    @Test
    fun theFigureOnTheStatisticsScreenIsTheSame() {
        studyOn(0, 1, 2)
        assertEquals(3, repo.stats(now).streakDays)
    }
}
