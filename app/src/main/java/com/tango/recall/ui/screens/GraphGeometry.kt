package com.tango.recall.ui.screens

import androidx.compose.ui.geometry.Offset
import kotlin.math.hypot

/**
 * The shapes the connection map is drawn from.
 *
 * Kept apart from the drawing so the geometry can be tested: a hull that collapses or
 * an arrow that points the wrong way is not something you want to discover by looking.
 */
object GraphGeometry {

    /**
     * The convex hull of a set of points, anticlockwise, by Andrew's monotone chain.
     *
     * Used to draw a soft shape behind a group of notes, so that a cluster reads as one
     * thing at a glance instead of as a handful of dots that happen to be near.
     */
    fun convexHull(points: List<Offset>): List<Offset> {
        val distinct = points.distinctBy { it.x to it.y }.sortedWith(compareBy({ it.x }, { it.y }))
        if (distinct.size <= 2) return distinct

        fun cross(o: Offset, a: Offset, b: Offset): Float =
            (a.x - o.x) * (b.y - o.y) - (a.y - o.y) * (b.x - o.x)

        fun half(source: List<Offset>): List<Offset> {
            val out = ArrayList<Offset>(source.size)
            for (point in source) {
                while (out.size >= 2 && cross(out[out.size - 2], out[out.size - 1], point) <= 0f) {
                    out.removeAt(out.size - 1)
                }
                out += point
            }
            out.removeAt(out.size - 1)
            return out
        }

        return half(distinct) + half(distinct.reversed())
    }

    /** The mean position of a set of points. */
    fun centroid(points: List<Offset>): Offset {
        if (points.isEmpty()) return Offset.Zero
        var x = 0f
        var y = 0f
        for (point in points) {
            x += point.x
            y += point.y
        }
        return Offset(x / points.size, y / points.size)
    }

    /**
     * Push a hull outwards so the shape sits around its notes rather than through them.
     *
     * A hull of one or two points has no area to expand, so it becomes a small ring
     * around them — which is what a cluster of two should look like anyway.
     */
    fun expand(hull: List<Offset>, padding: Float): List<Offset> {
        if (hull.isEmpty()) return hull
        val middle = centroid(hull)
        if (hull.size <= 2) {
            return (0 until 8).map { step ->
                val angle = step * 2.0 * Math.PI / 8
                val reach = padding + (hull.maxOfOrNull { distance(it, middle) } ?: 0f)
                Offset(
                    middle.x + (reach * kotlin.math.cos(angle)).toFloat(),
                    middle.y + (reach * kotlin.math.sin(angle)).toFloat(),
                )
            }
        }
        return hull.map { vertex ->
            val dx = vertex.x - middle.x
            val dy = vertex.y - middle.y
            val length = hypot(dx, dy).coerceAtLeast(0.001f)
            Offset(vertex.x + dx / length * padding, vertex.y + dy / length * padding)
        }
    }

    /** How spread out a group is: the mean distance of its notes from their centre. */
    fun spread(points: List<Offset>): Float {
        if (points.size < 2) return 0f
        val middle = centroid(points)
        return points.map { distance(it, middle) }.average().toFloat()
    }

    /**
     * The three corners of an arrowhead sitting at [tip], pointing away from [from],
     * pulled back by [gap] so it rests on the edge of the node rather than on top of it.
     */
    fun arrowHead(from: Offset, tip: Offset, size: Float, gap: Float): List<Offset> {
        val dx = tip.x - from.x
        val dy = tip.y - from.y
        val length = hypot(dx, dy).coerceAtLeast(0.001f)
        val ux = dx / length
        val uy = dy / length
        val point = Offset(tip.x - ux * gap, tip.y - uy * gap)
        val back = Offset(point.x - ux * size, point.y - uy * size)
        val halfWidth = size * 0.45f
        return listOf(
            point,
            Offset(back.x - uy * halfWidth, back.y + ux * halfWidth),
            Offset(back.x + uy * halfWidth, back.y - ux * halfWidth),
        )
    }


    /**
     * Is [point] inside [polygon]? Ray casting, counting crossings to the right.
     *
     * Used to keep group shapes from piling up: a group whose notes already sit inside
     * a shape that has been drawn does not get a second shape of its own.
     */
    fun contains(polygon: List<Offset>, point: Offset): Boolean {
        if (polygon.size < 3) return false
        var inside = false
        var j = polygon.size - 1
        for (i in polygon.indices) {
            val a = polygon[i]
            val b = polygon[j]
            if ((a.y > point.y) != (b.y > point.y)) {
                val x = a.x + (point.y - a.y) / (b.y - a.y) * (b.x - a.x)
                if (x > point.x) inside = !inside
            }
            j = i
        }
        return inside
    }

    /** One quadratic piece of a smoothed outline: bend towards [control], end at [end]. */
    data class Bend(val control: Offset, val end: Offset)

    /** A closed, smoothed outline: start at [start], then follow the [bends] round. */
    data class Blob(val start: Offset, val bends: List<Bend>)

    /**
     * Round a hull off into a blob.
     *
     * The corners of a convex hull are where its notes are, so a drawn hull has spikes
     * pointing at the very things it is meant to gather. Running the curve through the
     * edge midpoints and bending it around each corner gives the soft shape the eye
     * reads as "these belong together".
     */
    fun smooth(hull: List<Offset>): Blob? {
        if (hull.size < 3) return null
        fun midpoint(a: Offset, b: Offset) = Offset((a.x + b.x) / 2f, (a.y + b.y) / 2f)
        val start = midpoint(hull.last(), hull.first())
        val bends = hull.indices.map { i ->
            Bend(control = hull[i], end = midpoint(hull[i], hull[(i + 1) % hull.size]))
        }
        return Blob(start, bends)
    }

    private fun distance(a: Offset, b: Offset): Float = hypot(a.x - b.x, a.y - b.y)
}

/**
 * How much of the map is drawn, given how much room each note has on screen.
 *
 * A map of eight hundred notes cannot show eight hundred names, and does not need to:
 * pulled back, what is worth seeing is where the groups are; pushed in, it is which
 * note is which. The decision is made from the space between neighbouring notes rather
 * than from the zoom itself, because a map of twenty notes has room for every name at
 * the same zoom where a map of eight hundred has room for none.
 */
enum class MapDetail {
    /** Groups only: named shapes, small dots, faint links, no note names. */
    GROUPS,

    /** Groups and the hubs: the notes that hold a cluster together get their names. */
    HUBS,

    /** Everything that fits: names on every note the screen has room for. */
    NOTES;

    companion object {
        /**
         * @param spacing the mean gap between neighbouring notes, in dp — the same
         *   unit the names are sized in, so the thresholds mean the same thing on a
         *   dense screen as on a coarse one.
         */
        fun at(spacing: Float): MapDetail = when {
            spacing < 26f -> GROUPS
            spacing < 52f -> HUBS
            else -> NOTES
        }
    }
}
