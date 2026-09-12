package com.tango.recall

import com.tango.recall.notify.Reminders
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * When the daily reminder fires and what it says.
 *
 * The scheduling arithmetic is the part that can silently be a day out, and the
 * wording is what decides whether the notification gets read or swiped away.
 */
class ReminderTest {

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance().apply {
            clear()
            set(year, month - 1, day, hour, minute, 0)
        }.timeInMillis

    private fun fields(millis: Long): Triple<Int, Int, Int> {
        val c = Calendar.getInstance().apply { timeInMillis = millis }
        return Triple(c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))
    }

    @Test
    fun laterTodayIfTheTimeHasNotPassed() {
        val next = Reminders.nextTriggerAt(20, 0, at(2026, 9, 12, 8, 30))
        assertEquals(Triple(12, 20, 0), fields(next))
    }

    @Test
    fun tomorrowOnceItHas() {
        val next = Reminders.nextTriggerAt(20, 0, at(2026, 9, 12, 21, 15))
        assertEquals(Triple(13, 20, 0), fields(next))
    }

    @Test
    fun theTimeItselfCountsAsPassed() {
        val next = Reminders.nextTriggerAt(20, 0, at(2026, 9, 12, 20, 0))
        assertEquals("otherwise it would fire twice", Triple(13, 20, 0), fields(next))
    }

    @Test
    fun itCrossesTheMonthEnd() {
        val next = Reminders.nextTriggerAt(7, 30, at(2026, 9, 30, 23, 0))
        val c = Calendar.getInstance().apply { timeInMillis = next }
        assertEquals(Calendar.OCTOBER, c.get(Calendar.MONTH))
        assertEquals(1, c.get(Calendar.DAY_OF_MONTH))
        assertEquals(7, c.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun aNonsenseTimeIsClamped() {
        assertTrue(Reminders.nextTriggerAt(99, -5, at(2026, 9, 12, 8, 0)) > 0)
    }

    @Test
    fun aClearDaySaysNothingAtAll() {
        assertNull("a reminder with nothing to do teaches people to ignore it", Reminders.message(0, 0))
    }

    @Test
    fun theNumbersAreNamedSeparately() {
        assertEquals("復習 12 枚が待っています", Reminders.message(12, 0))
        assertEquals("新しい 5 枚が待っています", Reminders.message(0, 5))
        assertEquals("復習 12 枚と新しい 5 枚が待っています", Reminders.message(12, 5))
    }
}
