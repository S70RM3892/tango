package com.tango.recall

import androidx.compose.ui.geometry.Offset
import com.tango.recall.ui.screens.GraphGeometry
import com.tango.recall.ui.screens.MapDetail
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

/** The shapes behind the map: hulls around clusters, arrowheads on directed relations. */
class GraphGeometryTest {

    private fun square() = listOf(
        Offset(0f, 0f), Offset(10f, 0f), Offset(10f, 10f), Offset(0f, 10f), Offset(5f, 5f),
    )

    @Test
    fun theHullKeepsTheCornersAndDropsTheInside() {
        val hull = GraphGeometry.convexHull(square())
        assertEquals(4, hull.size)
        assertTrue("内側の点は輪郭に含まれない", hull.none { it == Offset(5f, 5f) })
        assertTrue(hull.contains(Offset(0f, 0f)) && hull.contains(Offset(10f, 10f)))
    }

    @Test
    fun collinearPointsDoNotCollapseTheHull() {
        val hull = GraphGeometry.convexHull(
            listOf(Offset(0f, 0f), Offset(5f, 0f), Offset(10f, 0f))
        )
        assertTrue("一直線でも両端は残る", hull.size >= 2)
    }

    @Test
    fun aHullOfOnePointBecomesARing() {
        val expanded = GraphGeometry.expand(GraphGeometry.convexHull(listOf(Offset(4f, 4f))), padding = 6f)
        assertTrue("点1つでも囲える形になる", expanded.size >= 6)
        for (point in expanded) {
            assertEquals(6f, hypot(point.x - 4f, point.y - 4f), 0.001f)
        }
    }

    @Test
    fun expandingPushesEveryCornerOutwards() {
        val hull = GraphGeometry.convexHull(square())
        val expanded = GraphGeometry.expand(hull, padding = 5f)
        val before = GraphGeometry.centroid(hull)
        val after = GraphGeometry.centroid(expanded)
        assertEquals("中心は動かない", before.x, after.x, 0.5f)
        for ((i, point) in expanded.withIndex()) {
            val original = hull[i]
            assertTrue(
                "外側に広がっていること",
                hypot(point.x - after.x, point.y - after.y) >
                    hypot(original.x - before.x, original.y - before.y),
            )
        }
    }

    @Test
    fun spreadGrowsWithTheCluster() {
        val tight = GraphGeometry.spread(listOf(Offset(0f, 0f), Offset(2f, 0f), Offset(0f, 2f)))
        val loose = GraphGeometry.spread(listOf(Offset(0f, 0f), Offset(50f, 0f), Offset(0f, 50f)))
        assertTrue(loose > tight)
        assertEquals("点1つに広がりはない", 0f, GraphGeometry.spread(listOf(Offset(3f, 3f))), 0f)
    }

    @Test
    fun theArrowPointsAlongTheRelationAndStopsShortOfTheNode() {
        val head = GraphGeometry.arrowHead(
            from = Offset(0f, 0f), tip = Offset(100f, 0f), size = 10f, gap = 8f,
        )
        assertEquals(3, head.size)
        assertEquals("先端は node の手前で止まる", 92f, head[0].x, 0.01f)
        assertTrue("残る2点は先端より後ろ", head[1].x < head[0].x && head[2].x < head[0].x)
        assertTrue("左右に開く", head[1].y * head[2].y < 0f)
    }

    @Test
    fun theArrowTurnsWithTheEdge() {
        val head = GraphGeometry.arrowHead(Offset(0f, 0f), Offset(0f, 100f), size = 10f, gap = 8f)
        assertEquals(92f, head[0].y, 0.01f)
        assertEquals(0f, head[0].x, 0.01f)
    }

    @Test
    fun detailFollowsHowMuchRoomEachNoteHas() {
        // 隣の語まで 16dp しかなければ名前は置けない。80dp あれば全部置ける。
        assertEquals(MapDetail.GROUPS, MapDetail.at(16f))
        assertEquals(MapDetail.HUBS, MapDetail.at(40f))
        assertEquals(MapDetail.NOTES, MapDetail.at(80f))
    }

    @Test
    fun aPointInsideTheSquareIsInsideAndOneOutsideIsNot() {
        val square = listOf(
            Offset(0f, 0f), Offset(10f, 0f), Offset(10f, 10f), Offset(0f, 10f),
        )
        assertTrue(GraphGeometry.contains(square, Offset(5f, 5f)))
        assertTrue(!GraphGeometry.contains(square, Offset(15f, 5f)))
        assertTrue(!GraphGeometry.contains(square, Offset(5f, -1f)))
        assertTrue("辺が2本では囲めない", !GraphGeometry.contains(square.take(2), Offset(5f, 5f)))
    }

    @Test
    fun smoothingRoundsTheCornersWithoutLeavingTheHull() {
        val square = listOf(
            Offset(0f, 0f), Offset(10f, 0f), Offset(10f, 10f), Offset(0f, 10f),
        )
        val blob = GraphGeometry.smooth(square)!!
        assertEquals(4, blob.bends.size)
        // 始点は最後の辺の中点、各区間の終点も辺の中点＝角そのものは通らない
        assertEquals(0f, blob.start.x, 0.01f)
        assertEquals(5f, blob.start.y, 0.01f)
        assertEquals(5f, blob.bends[0].end.x, 0.01f)
        assertEquals(0f, blob.bends[0].end.y, 0.01f)
        assertTrue("制御点は元の角", blob.bends.map { it.control } == square)
        assertEquals("三角形未満は丸められない", null, GraphGeometry.smooth(square.take(2)))
    }
}
