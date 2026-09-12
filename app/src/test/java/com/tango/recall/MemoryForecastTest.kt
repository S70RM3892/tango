package com.tango.recall

import com.tango.recall.data.CardMemory
import com.tango.recall.data.GraphNode
import com.tango.recall.srs.Fsrs
import com.tango.recall.ui.screens.horizonStops
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Predicting recall at a future date — what the map's time slider scrubs through. */
class MemoryForecastTest {

    private val day = 86_400_000L
    private val now = 1_800_000_000_000L

    private fun node(vararg memory: CardMemory) = GraphNode(
        noteId = 1, deckId = 1, typeId = "english", title = "t", subtitle = "",
        degree = 0, strength = 0.0, isNew = false, memory = memory.toList(),
    )

    @Test
    fun recallIsNinetyPercentAfterExactlyOneStability() {
        assertEquals(0.9, Fsrs.recallAfter(10.0, 10.0), 1e-9)
        assertEquals(0.9, Fsrs.recallAfter(365.0, 365.0), 1e-9)
    }

    @Test
    fun recallOnlyEverFalls() {
        var previous = 1.0
        for (days in 0..200 step 5) {
            val r = Fsrs.recallAfter(days.toDouble(), 30.0)
            assertTrue("recall rose between days", r <= previous + 1e-9)
            assertTrue(r in 0.0..1.0)
            previous = r
        }
    }

    @Test
    fun anUnlearnedOrJustReviewedCardIsHandled() {
        assertEquals(0.0, Fsrs.recallAfter(5.0, 0.0), 1e-9)
        assertEquals(1.0, Fsrs.recallAfter(0.0, 10.0), 1e-9)
        assertEquals(1.0, Fsrs.recallAfter(-3.0, 10.0), 1e-9)
    }

    @Test
    fun aStablerMemoryDecaysMoreSlowly() {
        assertTrue(Fsrs.recallAfter(30.0, 100.0) > Fsrs.recallAfter(30.0, 10.0))
    }

    @Test
    fun aNoteFadesAsTheSliderMovesForward() {
        val n = node(CardMemory(stability = 10.0, lastReview = now))
        val today = n.strengthAt(now)
        val inAWeek = n.strengthAt(now + 7 * day)
        val inAMonth = n.strengthAt(now + 30 * day)

        assertEquals(1.0, today, 1e-9)
        assertTrue(inAWeek < today)
        assertTrue(inAMonth < inAWeek)
        assertEquals(0.9, n.strengthAt(now + 10 * day), 1e-6)
    }

    @Test
    fun aNotesStrengthIsTheMeanOfItsCards() {
        val strong = CardMemory(stability = 1000.0, lastReview = now)
        val weak = CardMemory(stability = 1.0, lastReview = now - 30 * day)
        val both = node(strong, weak).strengthAt(now)
        assertTrue(both < node(strong).strengthAt(now))
        assertTrue(both > node(weak).strengthAt(now))
    }

    @Test
    fun aNoteWithNoHistoryReadsAsZero() {
        assertEquals(0.0, node().strengthAt(now), 1e-9)
        assertEquals(0.0, node(CardMemory(5.0, null)).strengthAt(now), 1e-9)
    }

    @Test
    fun theSliderAlwaysStartsAtTodayAndIsOrdered() {
        val stops = horizonStops(null)
        assertEquals(0, stops.first())
        assertEquals(stops.sorted(), stops)
        assertEquals(stops.distinct(), stops)
    }

    @Test
    fun theExamDayBecomesItsOwnStop() {
        val stops = horizonStops(45)
        assertTrue(45 in stops)
        assertEquals(stops.sorted(), stops)
        // A date that coincides with a standard stop must not be duplicated.
        assertEquals(horizonStops(null).size, horizonStops(30).size)
    }

    @Test
    fun anExamInThePastAddsNoStop() {
        assertEquals(horizonStops(null), horizonStops(0))
        assertEquals(horizonStops(null), horizonStops(-5))
    }
}
