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
    fun clozeBlanksAnInflectedPhrasalVerb() {
        assertEquals("She ______ running last spring.", blankOut("She took up running last spring.", "take up"))
        assertEquals(
            "The invention ______ a social change.",
            blankOut("The invention brought about a social change.", "bring about"),
        )
        assertEquals(
            "The country ______ a long recession.",
            blankOut("The country went through a long recession.", "go through"),
        )
    }

    @Test
    fun clozeBlanksAPhraseWithItsObjectInside() {
        assertEquals("______ before you decide.", blankOut("Think it over before you decide.", "think over"))
        assertEquals(
            "We must ______.",
            blankOut("We must take the delay into account.", "take A into account"),
        )
    }

    @Test
    fun clozeLeavesAnAmbiguousSentenceAlone() {
        // Two candidates for the verb: blanking the wrong one is worse than not blanking.
        val sentence = "He gave up smoking and took up running."
        assertTrue(blankOut(sentence, "take up").endsWith("（${CLOZE_BLANK}）"))
    }

    @Test
    fun clozeBlanksAnIdiomWrittenWithBe() {
        // The note says "be liable to"; the sentence says "are liable to".
        assertEquals(
            "Metal parts ______ rust in damp air.",
            blankOut("Metal parts are liable to rust in damp air.", "be liable to"),
        )
        assertEquals(
            "Prices ______ change without notice.",
            blankOut("Prices are subject to change without notice.", "be subject to"),
        )
        assertEquals(
            "Students should ______ the deadline.",
            blankOut("Students should be aware of the deadline.", "be aware of"),
        )
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
