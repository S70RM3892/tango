package com.tango.recall

import com.tango.recall.data.CLOZE_BLANK
import com.tango.recall.data.Grade
import com.tango.recall.data.blankOut
import com.tango.recall.data.gradeTyped
import com.tango.recall.data.humanDelay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentTest {

    @Test
    fun clozeBlanksTheExactWord() {
        val out = blankOut("He abandoned his plan.", "abandon")
        assertTrue(out.contains(CLOZE_BLANK))
        assertTrue("inflected forms must also be hidden", !out.contains("abandoned"))
    }

    @Test
    fun clozeKeepsTheRestOfTheSentence() {
        val out = blankOut("A quiet room is conducive to concentration.", "conducive")
        assertEquals("A quiet room is $CLOZE_BLANK to concentration.", out)
    }

    @Test
    fun clozeFallsBackWhenTheWordIsAbsent() {
        val out = blankOut("まったく無関係な文", "abandon")
        assertTrue(out.contains(CLOZE_BLANK))
    }

    @Test
    fun typedAnswerIgnoresCaseAndArticles() {
        assertEquals(Grade.CORRECT, gradeTyped("Abandon", "abandon").grade)
        assertEquals(Grade.CORRECT, gradeTyped("to abandon", "abandon").grade)
        assertEquals(Grade.CORRECT, gradeTyped("  postpone.", "postpone").grade)
    }

    @Test
    fun typedAnswerAcceptsAlternatives() {
        assertEquals(Grade.CORRECT, gradeTyped("retain", "retain / keep").grade)
        assertEquals(Grade.CORRECT, gradeTyped("keep", "retain / keep").grade)
    }

    @Test
    fun oneLetterTypoIsCloseNotWrong() {
        assertEquals(Grade.CLOSE, gradeTyped("abandom", "abandon").grade)
        assertEquals(Grade.WRONG, gradeTyped("banana", "abandon").grade)
        assertEquals(Grade.WRONG, gradeTyped("", "abandon").grade)
    }

    @Test
    fun chemistryFormulaToleratesSpacingAndDotStyle() {
        assertEquals(Grade.CORRECT, gradeTyped("CuSO4・5H2O", "CuSO4.5H2O", chemistry = true).grade)
        assertEquals(Grade.CORRECT, gradeTyped("2SO2 + O2 → 2SO3", "2SO2+O2->2SO3", chemistry = true).grade)
    }

    @Test
    fun chemistryStillRejectsAWrongFormula() {
        assertEquals(Grade.WRONG, gradeTyped("CuSO3", "CuSO4.5H2O", chemistry = true).grade)
    }

    @Test
    fun delaysAreReadable() {
        assertEquals("10分", humanDelay(10 * 60_000L))
        assertEquals("3日", humanDelay(3 * 86_400_000L))
        assertTrue(humanDelay(400L * 86_400_000L).endsWith("年"))
    }
}
