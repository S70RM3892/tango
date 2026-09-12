package com.tango.recall

import com.tango.recall.data.Department
import com.tango.recall.data.Universities
import com.tango.recall.data.UniversityFilter
import com.tango.recall.data.UniversitySort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UniversitiesTest {

    private val tsv = listOf(
        "university\tkind\tprefecture\tregion\tcity\tfaculty\tdepartment\tfield\tyears\tcapacity",
        "京都大学\t国立\t京都府\t近畿\t京都市\t工学部\t情報学科\t情報\t4\t90",
        "京都大学\t国立\t京都府\t近畿\t京都市\t理学部\t理学科\t理学\t4\t311",
        "東京大学\t国立\t東京都\t関東\t文京区\t工学部\t計数工学科\t情報\t4\t60",
        "大阪公立大学\t公立\t大阪府\t近畿\t大阪市\t工学部\t情報工学科\t情報\t4\t80",
        "壊れた行",
    )

    private val all = Universities.parse(tsv.asSequence())

    @Test
    fun theHeaderIsSkippedAndBrokenRowsAreIgnored() {
        assertEquals(4, all.size)
        assertEquals("京都大学", all.first().university)
        assertEquals(90, all.first().capacity)
    }

    @Test
    fun aDepartmentKeyIdentifiesItAcrossUniversities() {
        val keys = all.map { it.key }
        assertEquals(keys.size, keys.distinct().size)
        assertTrue(all.first().key.contains("京都大学"))
    }

    @Test
    fun anEmptyFilterKeepsEverything() {
        assertTrue(UniversityFilter().isEmpty)
        assertEquals(4, Universities.filter(all, UniversityFilter()).size)
    }

    @Test
    fun filtersNarrowAndCombine() {
        assertEquals(3, Universities.filter(all, UniversityFilter(fields = setOf("情報"))).size)
        assertEquals(1, Universities.filter(all, UniversityFilter(kinds = setOf("公立"))).size)
        assertEquals(3, Universities.filter(all, UniversityFilter(regions = setOf("近畿"))).size)
        assertEquals(
            2,
            Universities.filter(all, UniversityFilter(regions = setOf("近畿"), fields = setOf("情報"))).size,
        )
    }

    @Test
    fun theQueryLooksAtNamePlaceAndDepartment() {
        assertEquals(2, Universities.filter(all, UniversityFilter(query = "京都大")).size)
        assertEquals(1, Universities.filter(all, UniversityFilter(query = "計数")).size)
        assertEquals(1, Universities.filter(all, UniversityFilter(query = "東京都")).size)
        assertTrue(Universities.filter(all, UniversityFilter(query = "存在しない")).isEmpty())
    }

    @Test
    fun theCapacityFloorExcludesSmallDepartments() {
        assertEquals(2, Universities.filter(all, UniversityFilter(minCapacity = 85)).size)
    }

    @Test
    fun groupingSumsOnlyTheDepartmentsThatSurvivedTheFilter() {
        val everything = Universities.groupByUniversity(all, UniversitySort.CAPACITY_DESC)
        assertEquals(401, everything.first { it.university == "京都大学" }.capacity)

        // Narrowed to 情報, Kyoto should count only its 90 places, not all 401.
        val informatics = Universities.filter(all, UniversityFilter(fields = setOf("情報")))
        val grouped = Universities.groupByUniversity(informatics, UniversitySort.CAPACITY_DESC)
        assertEquals(90, grouped.first { it.university == "京都大学" }.capacity)
        assertEquals(listOf("京都大学", "大阪公立大学", "東京大学"), grouped.map { it.university })
    }

    @Test
    fun everySortOrdersDifferentlyButKeepsTheSameSet() {
        val names = UniversitySort.entries.map { sort ->
            Universities.groupByUniversity(all, sort).map { it.university }
        }
        names.forEach { assertEquals(3, it.size) }
        assertEquals("京都大学", names[UniversitySort.CAPACITY_DESC.ordinal].first())
        assertEquals("東京大学", names[UniversitySort.CAPACITY_ASC.ordinal].first())
        assertEquals(listOf("京都大学", "大阪公立大学", "東京大学"), names[UniversitySort.UNIVERSITY.ordinal])
        // Sorted by place, Kanto comes before Kinki.
        assertEquals("東京大学", names[UniversitySort.PREFECTURE.ordinal].first())
    }

    @Test
    fun prefectureTotalsAddUp() {
        val byPrefecture = Universities.capacityByPrefecture(all)
        assertEquals(401, byPrefecture["京都府"])
        assertEquals(60, byPrefecture["東京都"])
        assertEquals(all.sumOf { it.capacity }, byPrefecture.values.sum())
    }

    @Test
    fun fieldTotalsAreRankedBySize() {
        val composition = Universities.capacityByField(all)
        assertEquals("理学", composition.first().first)
        assertEquals(311, composition.first().second)
        assertEquals(230, composition.first { it.first == "情報" }.second)
    }

    @Test
    fun groupingAnEmptySelectionIsEmptyRatherThanAnError() {
        assertTrue(Universities.groupByUniversity(emptyList(), UniversitySort.CAPACITY_DESC).isEmpty())
        assertTrue(Universities.capacityByPrefecture(emptyList()).isEmpty())
    }
}
