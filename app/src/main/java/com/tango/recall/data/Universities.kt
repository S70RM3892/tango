package com.tango.recall.data

/**
 * One department (学科) of a national or public university.
 *
 * Source: 文部科学省「令和7年度 全国大学一覧」. Departments that have stopped
 * recruiting are excluded at build time, so every row here is somewhere you can
 * actually apply.
 */
data class Department(
    val university: String,
    val kind: String,
    val prefecture: String,
    val region: String,
    val city: String,
    val faculty: String,
    val department: String,
    val field: String,
    val years: Int,
    val capacity: Int,
) {
    val label: String get() = "$faculty $department"

    /** Stable identity for the shortlist, independent of row order. */
    val key: String get() = "$university\u0001$faculty\u0001$department"
}

/** All universities rolled up from their departments, for the list view. */
data class UniversityGroup(
    val university: String,
    val kind: String,
    val prefecture: String,
    val region: String,
    val capacity: Int,
    val departments: List<Department>,
)

data class UniversityFilter(
    val query: String = "",
    /** Empty means "no restriction" for each of these. */
    val kinds: Set<String> = emptySet(),
    val regions: Set<String> = emptySet(),
    val prefectures: Set<String> = emptySet(),
    val fields: Set<String> = emptySet(),
    val minCapacity: Int = 0,
) {
    val isEmpty: Boolean
        get() = query.isBlank() && kinds.isEmpty() && regions.isEmpty() &&
            prefectures.isEmpty() && fields.isEmpty() && minCapacity <= 0
}

enum class UniversitySort(val label: String) {
    CAPACITY_DESC("定員の多い順"),
    CAPACITY_ASC("定員の少ない順"),
    UNIVERSITY("大学名順"),
    PREFECTURE("所在地順"),
}

object Universities {

    const val ASSET = "universities.tsv"

    val KINDS = listOf("国立", "公立")

    val REGIONS = listOf("北海道", "東北", "関東", "中部", "近畿", "中国", "四国", "九州沖縄")

    val FIELDS = listOf(
        "情報", "工学", "理学", "医学", "歯学", "薬学", "獣医", "看護・保健", "農・水産",
        "教育", "法学", "経済・経営", "文・人文", "社会・国際", "芸術", "体育", "総合・学際", "その他",
    )

    /** Read the bundled table. The header row is skipped; malformed rows are ignored. */
    fun parse(lines: Sequence<String>): List<Department> = lines
        .drop(1)
        .mapNotNull { line ->
            val c = line.split('\t')
            if (c.size < 10) return@mapNotNull null
            Department(
                university = c[0], kind = c[1], prefecture = c[2], region = c[3], city = c[4],
                faculty = c[5], department = c[6], field = c[7],
                years = c[8].toIntOrNull() ?: 0,
                capacity = c[9].toIntOrNull() ?: 0,
            )
        }
        .toList()

    fun filter(all: List<Department>, filter: UniversityFilter): List<Department> {
        val needle = filter.query.trim().lowercase()
        return all.filter { d ->
            (filter.kinds.isEmpty() || d.kind in filter.kinds) &&
                (filter.regions.isEmpty() || d.region in filter.regions) &&
                (filter.prefectures.isEmpty() || d.prefecture in filter.prefectures) &&
                (filter.fields.isEmpty() || d.field in filter.fields) &&
                d.capacity >= filter.minCapacity &&
                (
                    needle.isEmpty() ||
                        d.university.lowercase().contains(needle) ||
                        d.faculty.lowercase().contains(needle) ||
                        d.department.lowercase().contains(needle) ||
                        d.prefecture.lowercase().contains(needle)
                    )
        }
    }

    /**
     * Roll departments up to their universities.
     *
     * Capacity is summed over the departments that survived the filter, not over the
     * whole university — so filtering to 情報 and sorting by capacity ranks by how much
     * of that subject each place actually takes.
     */
    fun groupByUniversity(list: List<Department>, sort: UniversitySort): List<UniversityGroup> {
        val groups = list.groupBy { it.university }.map { (name, departments) ->
            val head = departments.first()
            UniversityGroup(
                university = name,
                kind = head.kind,
                prefecture = head.prefecture,
                region = head.region,
                capacity = departments.sumOf { it.capacity },
                departments = departments.sortedByDescending { it.capacity },
            )
        }
        return when (sort) {
            UniversitySort.CAPACITY_DESC -> groups.sortedWith(
                compareByDescending<UniversityGroup> { it.capacity }.thenBy { it.university }
            )
            UniversitySort.CAPACITY_ASC -> groups.sortedWith(
                compareBy<UniversityGroup> { it.capacity }.thenBy { it.university }
            )
            UniversitySort.UNIVERSITY -> groups.sortedBy { it.university }
            UniversitySort.PREFECTURE -> groups.sortedWith(
                compareBy<UniversityGroup> { REGIONS.indexOf(it.region) }
                    .thenBy { it.prefecture }
                    .thenBy { it.university }
            )
        }
    }

    /** Total places per prefecture, which is what shades the map. */
    fun capacityByPrefecture(list: List<Department>): Map<String, Int> =
        list.groupingBy { it.prefecture }.fold(0) { sum, d -> sum + d.capacity }

    fun capacityByField(list: List<Department>): List<Pair<String, Int>> =
        list.groupingBy { it.field }.fold(0) { sum, d -> sum + d.capacity }
            .toList().sortedByDescending { it.second }
}
