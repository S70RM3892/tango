package com.tango.recall

import com.tango.recall.ui.screens.shortLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShortLabelTest {

    @Test
    fun shortTitlesAreLeftAlone() {
        assertEquals("abandon", shortLabel("abandon"))
        assertEquals("硫酸", shortLabel("硫酸"))
    }

    @Test
    fun japaneseIsBudgetedAtTwiceTheWidth() {
        // 11 wide characters is 22 units, so the eleventh does not fit alongside a "…".
        assertEquals("アンモニアソーダ法（ソ…", shortLabel("アンモニアソーダ法（ソルベー法）"))
        assertTrue(shortLabel("アンモニアソーダ法（ソルベー法）").endsWith("…"))
    }

    @Test
    fun latinTitlesKeepAboutTwiceAsManyCharacters() {
        assertEquals("unprecedented", shortLabel("unprecedented"))
        assertEquals("abcdefghijklmnopqrstuv", shortLabel("abcdefghijklmnopqrstuv"))
        assertEquals("abcdefghijklmnopqrstuv…", shortLabel("abcdefghijklmnopqrstuvwxyz"))
    }

    @Test
    fun aMixedTitleKeepsTheInformativeLatinPrefix() {
        // The old character-count rule cut this to "0.010 mo…", which says nothing.
        val label = shortLabel("0.010 mol/L の塩酸の pH はいくらか。")
        assertTrue(label, label.startsWith("0.010 mol/L"))
        assertTrue(label, label.endsWith("…"))
    }

    @Test
    fun theBudgetIsNeverExceeded() {
        fun width(s: String) = s.sumOf { c ->
            if (c.code in 0x2E80..0xA4CF || c.code in 0xFF00..0xFF60) 2 else 1
        }
        listOf(
            "接触法（硫酸の工業的製法）",
            "彼女は口を開けば人の悪口ばかりだ。",
            "27℃、1.0×10^5 Pa で 2.0 mol の理想気体が占める体積は何 L か。",
            "short",
        ).forEach { assertTrue(it, width(shortLabel(it)) <= 23) }
    }

    @Test
    fun emptyAndWhitespaceTitlesSurvive() {
        assertEquals("", shortLabel(""))
        assertEquals("", shortLabel("   "))
    }
}
