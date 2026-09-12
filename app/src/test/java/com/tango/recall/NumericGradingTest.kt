package com.tango.recall

import com.tango.recall.data.Grade
import com.tango.recall.data.gradeNumeric
import com.tango.recall.data.gradeSelfCheck
import com.tango.recall.data.parseNumber
import com.tango.recall.srs.Rating
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NumericGradingTest {

    @Test
    fun parsesTheWaysChemistryAnswersGetWritten() {
        assertEquals(0.25, parseNumber("0.25")!!, 1e-12)
        assertEquals(-3.5, parseNumber("-3.5")!!, 1e-12)
        assertEquals(1.2e-3, parseNumber("1.2e-3")!!, 1e-15)
        assertEquals(2.7e-5, parseNumber("2.7×10^-5")!!, 1e-18)
        assertEquals(2.7e-5, parseNumber("2.7*10^-5")!!, 1e-18)
        assertEquals(1.0e5, parseNumber("10^5")!!, 1e-6)
        assertEquals(1234.0, parseNumber("1,234")!!, 1e-9)
        assertEquals(50.0, parseNumber("５０")!!, 1e-9)
        assertNull(parseNumber("だいたい 50 くらい"))
    }

    @Test
    fun acceptsAnswersInsideTheTolerance() {
        assertEquals(Grade.CORRECT, gradeNumeric("0.25", "0.25", 1.0, "mol").grade)
        assertEquals(Grade.CORRECT, gradeNumeric("0.2502", "0.25", 1.0, "mol").grade)
        assertEquals(Grade.CORRECT, gradeNumeric("49.8", "50", 2.0, "L").grade)
        assertEquals(Grade.CORRECT, gradeNumeric("1.6e-3", "1.6×10^-3", 2.0).grade)
    }

    @Test
    fun commentsOnSignificantFiguresWithoutMarkingItWrong() {
        val result = gradeNumeric("0.4", "0.400", 1.0, "mol/L")
        assertEquals(Grade.CORRECT, result.grade)
        assertTrue(result.comment, result.comment.contains("有効数字"))
    }

    @Test
    fun anAnswerOffByAPowerOfTenIsWrongButSaysSo() {
        val result = gradeNumeric("2.5", "0.25", 1.0, "mol")
        assertEquals(Grade.WRONG, result.grade)
        assertTrue(result.comment, result.comment.contains("桁"))
        assertEquals(Rating.AGAIN, result.suggestedRating)
    }

    @Test
    fun aSmallMissIsCloseAndABigOneIsWrong() {
        assertEquals(Grade.CLOSE, gradeNumeric("0.252", "0.25", 0.5).grade)
        assertEquals(Grade.WRONG, gradeNumeric("0.9", "0.25", 1.0).grade)
    }

    @Test
    fun unreadableAndEmptyInputAreWrong() {
        assertEquals(Grade.WRONG, gradeNumeric("わからない", "0.25", 1.0).grade)
        assertEquals(Grade.WRONG, gradeNumeric("", "0.25", 1.0).grade)
    }

    @Test
    fun theUnitIsShownWithTheExpectedAnswer() {
        assertEquals("0.25 mol", gradeNumeric("9", "0.25", 1.0, "mol").expected)
    }

    @Test
    fun aNonNumericExpectedAnswerFallsBackToTextComparison() {
        assertEquals(Grade.CORRECT, gradeNumeric("Cu(NO3)2", "Cu(NO3)2", 1.0).grade)
    }

    @Test
    fun selfCheckScoresByHowManyPointsWereCovered() {
        assertEquals(Grade.CORRECT, gradeSelfCheck(3, 3).grade)
        assertEquals(Rating.GOOD, gradeSelfCheck(3, 3).suggestedRating)
        assertEquals(Grade.CLOSE, gradeSelfCheck(2, 3).grade)
        assertEquals(Grade.WRONG, gradeSelfCheck(0, 3).grade)
        assertTrue(gradeSelfCheck(2, 3).comment.contains("3 点中 2 点"))
    }
}
