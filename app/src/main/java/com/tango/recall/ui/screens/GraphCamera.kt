package com.tango.recall.ui.screens

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

/**
 * The pan/zoom transform of the connection map.
 *
 * Kept as a plain value type with no Compose state in it, so the awkward parts —
 * anchoring a pinch to the point between the fingers, clamping, fitting the whole
 * graph on screen — are ordinary functions that can be tested without a device.
 */
data class GraphCamera(
    val scale: Float,
    val offset: Offset,
) {
    fun toScreen(point: Offset): Offset = Offset(point.x * scale + offset.x, point.y * scale + offset.y)

    fun toGraph(point: Offset): Offset = Offset((point.x - offset.x) / scale, (point.y - offset.y) / scale)

    fun panned(delta: Offset): GraphCamera = copy(offset = offset + delta)

    /**
     * Scale by [factor] while keeping whatever sits under [pivot] pinned to [pivot].
     *
     * This is what makes a pinch feel right: the content under your fingers must not
     * slide away. Clamping happens before the offset is solved, so hitting the zoom
     * limit does not drift the map.
     */
    fun zoomedAround(pivot: Offset, factor: Float, minScale: Float, maxScale: Float): GraphCamera {
        if (!factor.isFinite() || factor <= 0f) return this
        val target = (scale * factor).coerceIn(minScale, maxScale)
        if (target == scale) return this
        // Solve  pivot = graphPoint * target + newOffset  for newOffset.
        val graphPoint = toGraph(pivot)
        return GraphCamera(
            scale = target,
            offset = Offset(pivot.x - graphPoint.x * target, pivot.y - graphPoint.y * target),
        )
    }

    /** Move [graphPoint] to the middle of a [viewport]-sized window. */
    fun centredOn(graphPoint: Offset, viewport: Size): GraphCamera = copy(
        offset = Offset(
            viewport.width / 2f - graphPoint.x * scale,
            viewport.height / 2f - graphPoint.y * scale,
        )
    )

    companion object {
        val Identity = GraphCamera(1f, Offset.Zero)

        /** Scale and centre [content] so all of it is visible inside [viewport]. */
        fun fit(content: Size, viewport: Size, padding: Float = 0f): GraphCamera {
            if (viewport.width <= 0f || viewport.height <= 0f) return Identity
            val usableWidth = (viewport.width - padding * 2).coerceAtLeast(1f)
            val usableHeight = (viewport.height - padding * 2).coerceAtLeast(1f)
            val scale = if (content.width <= 0f || content.height <= 0f) 1f
            else minOf(usableWidth / content.width, usableHeight / content.height)
            return GraphCamera(
                scale = scale,
                offset = Offset(
                    (viewport.width - content.width * scale) / 2f,
                    (viewport.height - content.height * scale) / 2f,
                ),
            )
        }
    }
}
