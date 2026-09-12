package com.tango.recall

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.tango.recall.ui.screens.GraphCamera
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The map shipped once with a pinch that did nothing, because the gesture handler
 * read a stale scale. These pin the transform arithmetic itself.
 */
class GraphCameraTest {

    private fun assertOffsetEquals(expected: Offset, actual: Offset, tolerance: Float = 1e-3f) {
        assertEquals("x", expected.x, actual.x, tolerance)
        assertEquals("y", expected.y, actual.y, tolerance)
    }

    @Test
    fun screenAndGraphCoordinatesRoundTrip() {
        val camera = GraphCamera(2.5f, Offset(-40f, 90f))
        val point = Offset(123f, -45f)
        assertOffsetEquals(point, camera.toGraph(camera.toScreen(point)))
    }

    @Test
    fun pinchKeepsThePointBetweenTheFingersStill() {
        val camera = GraphCamera(1f, Offset(30f, 20f))
        val pivot = Offset(400f, 700f)
        val before = camera.toGraph(pivot)

        val zoomed = camera.zoomedAround(pivot, 2.3f, 0.2f, 8f)

        assertEquals(2.3f, zoomed.scale, 1e-4f)
        // The same graph point must still be under the fingers afterwards.
        assertOffsetEquals(pivot, zoomed.toScreen(before))
    }

    @Test
    fun repeatedPinchStepsCompound() {
        // The shipped bug: each gesture event multiplied a frozen 1.0 instead of the
        // running scale, so many small steps never added up.
        var camera = GraphCamera.Identity
        repeat(10) { camera = camera.zoomedAround(Offset(200f, 200f), 1.1f, 0.2f, 8f) }
        assertEquals(Math.pow(1.1, 10.0).toFloat(), camera.scale, 1e-3f)
    }

    @Test
    fun pinchingOutThenBackReturnsToWhereItStarted() {
        val start = GraphCamera(1.4f, Offset(12f, -8f))
        val pivot = Offset(320f, 540f)
        val there = start.zoomedAround(pivot, 3f, 0.2f, 8f)
        val back = there.zoomedAround(pivot, 1f / 3f, 0.2f, 8f)
        assertEquals(start.scale, back.scale, 1e-4f)
        assertOffsetEquals(start.offset, back.offset, 1e-2f)
    }

    @Test
    fun zoomClampsWithoutDriftingTheMap() {
        val camera = GraphCamera(8f, Offset(5f, 5f))
        val clamped = camera.zoomedAround(Offset(100f, 100f), 4f, 0.2f, 8f)
        assertEquals(8f, clamped.scale, 1e-6f)
        // Already at the limit: nothing at all should move.
        assertOffsetEquals(camera.offset, clamped.offset)
    }

    @Test
    fun degenerateZoomFactorsAreIgnored() {
        val camera = GraphCamera(2f, Offset(1f, 2f))
        assertEquals(camera, camera.zoomedAround(Offset(10f, 10f), 0f, 0.2f, 8f))
        assertEquals(camera, camera.zoomedAround(Offset(10f, 10f), Float.NaN, 0.2f, 8f))
    }

    @Test
    fun panningIsAdditiveAndShiftsContentOneForOne() {
        val camera = GraphCamera(3f, Offset.Zero)
        val moved = camera.panned(Offset(10f, -5f)).panned(Offset(4f, 5f))
        assertOffsetEquals(Offset(14f, 0f), moved.offset)
        // Scale is untouched by panning, so content moves exactly with the finger.
        assertEquals(3f, moved.scale, 1e-6f)
        assertOffsetEquals(
            camera.toScreen(Offset(2f, 2f)) + Offset(14f, 0f),
            moved.toScreen(Offset(2f, 2f)),
        )
    }

    @Test
    fun fitShowsTheWholeGraphAndCentresIt() {
        val content = Size(1000f, 1000f)
        val viewport = Size(600f, 900f)
        val camera = GraphCamera.fit(content, viewport, padding = 24f)

        val topLeft = camera.toScreen(Offset.Zero)
        val bottomRight = camera.toScreen(Offset(content.width, content.height))

        assertTrue("content overflows the viewport", topLeft.x >= -0.01f && topLeft.y >= -0.01f)
        assertTrue(bottomRight.x <= viewport.width + 0.01f)
        assertTrue(bottomRight.y <= viewport.height + 0.01f)
        // Centred: equal gaps on opposite sides.
        assertEquals(topLeft.x, viewport.width - bottomRight.x, 0.01f)
        assertEquals(topLeft.y, viewport.height - bottomRight.y, 0.01f)
    }

    @Test
    fun fitHandlesAnEmptyViewportOrEmptyContent() {
        assertEquals(GraphCamera.Identity, GraphCamera.fit(Size(100f, 100f), Size.Zero))
        assertEquals(1f, GraphCamera.fit(Size.Zero, Size(100f, 100f)).scale, 1e-6f)
    }

    @Test
    fun centringPutsTheChosenPointInTheMiddle() {
        val camera = GraphCamera(2f, Offset(999f, -999f)).centredOn(Offset(50f, 80f), Size(400f, 600f))
        assertOffsetEquals(Offset(200f, 300f), camera.toScreen(Offset(50f, 80f)))
        assertEquals(2f, camera.scale, 1e-6f)
    }
}
