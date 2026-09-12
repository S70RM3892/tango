package com.tango.recall

import androidx.test.core.app.ApplicationProvider
import androidx.compose.ui.geometry.Offset
import com.tango.recall.data.Universities
import com.tango.recall.ui.screens.JapanMap
import com.tango.recall.ui.screens.JapanMapLoader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Checks the data the app actually ships, not just the code that reads it.
 *
 * A wrong figure here would send someone to the wrong university, so the assertions
 * are about the content: totals in a plausible range, merged universities gone,
 * and the map lining up with the table.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BundledDataTest {

    private val assets = ApplicationProvider.getApplicationContext<android.app.Application>().assets

    private val departments by lazy {
        assets.open(Universities.ASSET).bufferedReader().useLines { Universities.parse(it) }
    }

    private val map: JapanMap by lazy {
        assets.open(JapanMapLoader.ASSET).bufferedReader().useLines { JapanMapLoader.parse(it) }
    }

    @Test
    fun theTableCoversEveryNationalAndPublicUniversity() {
        assertTrue("only ${departments.size} departments", departments.size > 1200)
        val universities = departments.map { it.university }.distinct()
        assertTrue("only ${universities.size} universities", universities.size in 150..200)
        assertEquals(47, departments.map { it.prefecture }.distinct().size)
    }

    @Test
    fun totalCapacityMatchesTheKnownNationalFigure() {
        // National plus public undergraduate intake is a little over 130,000 a year.
        val total = departments.sumOf { it.capacity }
        assertTrue("total intake was $total", total in 120_000..140_000)
        assertTrue("national places should outnumber public ones",
            departments.filter { it.kind == "国立" }.sumOf { it.capacity } >
                departments.filter { it.kind == "公立" }.sumOf { it.capacity })
    }

    @Test
    fun everyRowIsSomewhereYouCanActuallyApply() {
        assertTrue("a department with no intake is not open to applicants",
            departments.none { it.capacity <= 0 })
        assertTrue(departments.none { it.department.contains("共通") })
        assertTrue(departments.all { it.kind in Universities.KINDS })
        assertTrue(departments.all { it.region in Universities.REGIONS })
        assertTrue(departments.all { it.field in Universities.FIELDS })
        assertTrue(departments.all { it.university.isNotBlank() && it.faculty.isNotBlank() })
    }

    @Test
    fun mergedUniversitiesAreGoneAndTheirSuccessorIsPresent() {
        // Osaka City and Osaka Prefecture merged into Osaka Metropolitan in 2022; both
        // still appear in the ministry's list for students already enrolled.
        assertTrue(departments.none { it.university == "大阪市立大学" })
        assertTrue(departments.none { it.university == "大阪府立大学" })
        assertTrue(departments.any { it.university == "大阪公立大学" })
    }

    @Test
    fun aKnownDepartmentHasTheRightFigures() {
        val informatics = departments.single {
            it.university == "京都大学" && it.faculty == "工学部" && it.department == "情報学科"
        }
        assertEquals(90, informatics.capacity)
        assertEquals("京都府", informatics.prefecture)
        assertEquals("近畿", informatics.region)
        assertEquals("情報", informatics.field)
        assertEquals(4, informatics.years)
    }

    @Test
    fun informaticsIsFindableAcrossTheCountry() {
        val informatics = departments.filter { it.field == "情報" }
        assertTrue("only ${informatics.size} informatics departments", informatics.size > 80)
        assertTrue(informatics.map { it.region }.distinct().size >= 7)
    }

    @Test
    fun theMapHasEveryPrefectureTheTableMentions() {
        assertEquals(47, map.prefectures.size)
        val drawn = map.prefectures.map { it.name }.toSet()
        val listed = departments.map { it.prefecture }.toSet()
        assertTrue("missing from the map: ${listed - drawn}", (listed - drawn).isEmpty())
    }

    @Test
    fun theMapIsOrientedCorrectly() {
        fun anchor(name: String) = map.prefectures.first { it.name == name }.labelAnchor
        // y grows southwards, x eastwards.
        assertTrue(anchor("北海道").y < anchor("東京都").y)
        assertTrue(anchor("東京都").y < anchor("鹿児島県").y)
        assertTrue(anchor("福岡県").x < anchor("東京都").x)
        assertTrue(anchor("東京都").x > anchor("京都府").x)
    }

    @Test
    fun tappingInsideAPrefectureFindsIt() {
        var matched = 0
        for (shape in map.prefectures) {
            if (map.prefectureAt(shape.labelAnchor) == shape.name) matched++
        }
        // Concave prefectures can have a centroid outside their own outline; most
        // must still resolve to themselves or tapping the map would feel broken.
        assertTrue("only $matched of 47 resolved", matched >= 40)
    }

    @Test
    fun tappingTheOpenSeaSelectsNothing() {
        assertNull(map.prefectureAt(Offset(-50f, -50f)))
        assertNull(map.prefectureAt(Offset(map.size.width + 100f, map.size.height + 100f)))
    }

    @Test
    fun okinawaIsDrawnInsideItsInsetBox() {
        val inset = requireNotNull(map.inset)
        val okinawa = map.prefectures.first { it.name == "沖縄県" }
        assertTrue("Okinawa should sit inside the inset frame", inset.contains(okinawa.bounds.center))
        // And the inset must not overlap the main islands.
        assertNull(map.prefectureAt(Offset(inset.left - 5f, inset.top - 5f)))
    }
}
