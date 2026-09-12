package com.tango.recall

import com.tango.recall.ui.screens.firstUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Finding the link in a note's 出典 field.
 *
 * The field is written by hand, so the address sits in the middle of Japanese text and
 * has to be picked out without swallowing the punctuation that follows it.
 */
class LinkTest {

    @Test
    fun aPlainAddressIsFound() {
        assertEquals(
            "https://www.kyoto-u.ac.jp/ja/admissions/undergrad/past-eq/r7-eq",
            firstUrl("https://www.kyoto-u.ac.jp/ja/admissions/undergrad/past-eq/r7-eq"),
        )
    }

    @Test
    fun japanesePunctuationIsNotPartOfTheAddress() {
        assertEquals(
            "https://example.jp/a/b",
            firstUrl("令和7年度 理系数学（https://example.jp/a/b）第3問"),
        )
        assertEquals("https://example.jp/x", firstUrl("出典: https://example.jp/x。第2問"))
    }

    @Test
    fun textWithoutALinkHasNone() {
        assertNull(firstUrl("自作（出題の型に沿った練習）"))
        assertNull(firstUrl(""))
    }

    @Test
    fun theFirstOfSeveralWins() {
        assertEquals("http://a.jp", firstUrl("http://a.jp と https://b.jp"))
    }
}
