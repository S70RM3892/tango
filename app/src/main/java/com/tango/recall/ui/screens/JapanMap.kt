package com.tango.recall.ui.screens

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size

/** One prefecture's outline, already projected into the map's own coordinate box. */
data class PrefectureShape(
    val name: String,
    val polygons: List<List<Offset>>,
) {
    val bounds: Rect = polygons.flatten().let { points ->
        if (points.isEmpty()) Rect.Zero
        else Rect(
            points.minOf { it.x }, points.minOf { it.y },
            points.maxOf { it.x }, points.maxOf { it.y },
        )
    }

    /** Centre of the largest island, used to place the label. */
    val labelAnchor: Offset = polygons.maxByOrNull { it.size }?.let { ring ->
        Offset(ring.sumOf { it.x.toDouble() }.toFloat() / ring.size,
            ring.sumOf { it.y.toDouble() }.toFloat() / ring.size)
    } ?: bounds.center

    /** Ray casting: a point is inside if a ray crosses the outline an odd number of times. */
    fun contains(point: Offset): Boolean {
        if (!bounds.contains(point)) return false
        return polygons.any { ring ->
            var inside = false
            var j = ring.size - 1
            for (i in ring.indices) {
                val a = ring[i]
                val b = ring[j]
                if ((a.y > point.y) != (b.y > point.y) &&
                    point.x < (b.x - a.x) * (point.y - a.y) / (b.y - a.y) + a.x
                ) inside = !inside
                j = i
            }
            inside
        }
    }
}

/**
 * The outline of Japan.
 *
 * Okinawa is drawn in an inset box at unchanged scale, as Japanese maps conventionally
 * do; only its position is a convention. Islands more than a few hundred kilometres
 * from the main chain (Ogasawara, Amami, Tokara) are left out so the frame is not
 * stretched for a handful of specks.
 */
data class JapanMap(
    val prefectures: List<PrefectureShape>,
    val inset: Rect?,
    val size: Size,
) {
    fun prefectureAt(point: Offset): String? = prefectures.firstOrNull { it.contains(point) }?.name

    companion object {
        val Empty = JapanMap(emptyList(), null, Size.Zero)
    }
}

object JapanMapLoader {

    const val ASSET = "japan_map.txt"

    /** Source: 地球地図日本（国土地理院）, simplified at build time. */
    const val ATTRIBUTION = "地図データ: 地球地図日本（国土地理院）"

    fun parse(lines: Sequence<String>): JapanMap {
        var inset: Rect? = null
        val shapes = mutableListOf<PrefectureShape>()

        for (line in lines) {
            if (line.isBlank()) continue
            val parts = line.split('|')
            if (parts[0] == "@inset") {
                val box = parts.getOrNull(1)?.split(',')?.mapNotNull { it.toFloatOrNull() }
                if (box != null && box.size == 4) {
                    inset = Rect(box[0], box[1], box[0] + box[2], box[1] + box[3])
                }
                continue
            }
            val polygons = parts.drop(1).mapNotNull { polygon ->
                val points = polygon.split(' ').mapNotNull { pair ->
                    val xy = pair.split(',')
                    if (xy.size != 2) return@mapNotNull null
                    val x = xy[0].toFloatOrNull() ?: return@mapNotNull null
                    val y = xy[1].toFloatOrNull() ?: return@mapNotNull null
                    Offset(x, y)
                }
                points.takeIf { it.size >= 3 }
            }
            if (polygons.isNotEmpty()) shapes += PrefectureShape(parts[0], polygons)
        }

        val all = shapes.flatMap { it.polygons.flatten() }
        val size = if (all.isEmpty()) Size.Zero
        else Size(all.maxOf { it.x }, all.maxOf { it.y })
        return JapanMap(shapes, inset, size)
    }
}
