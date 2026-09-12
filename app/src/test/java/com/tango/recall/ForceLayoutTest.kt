package com.tango.recall

import com.tango.recall.data.GraphData
import com.tango.recall.data.GraphEdge
import com.tango.recall.data.GraphNode
import com.tango.recall.ui.screens.ForceLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

class ForceLayoutTest {

    private fun node(id: Long, degree: Int = 1) = GraphNode(
        noteId = id, deckId = 1, typeId = "english",
        title = "n$id", subtitle = "", degree = degree, strength = 0.8, isNew = false,
    )

    private fun ring(count: Int): GraphData {
        val nodes = (1..count).map { node(it.toLong(), 2) }
        val edges = (1..count).map { GraphEdge(it.toLong(), (it % count + 1).toLong(), "same_root", "同語根") }
        return GraphData(nodes, edges)
    }

    @Test
    fun everyNodeLandsInsideTheBoxWithRealCoordinates() {
        val laid = ForceLayout.layout(ring(24))
        assertEquals(24, laid.nodes.size)
        for (positioned in laid.nodes) {
            assertTrue("NaN position", !positioned.x.isNaN() && !positioned.y.isNaN())
            assertTrue(positioned.x in 0f..laid.width)
            assertTrue(positioned.y in 0f..laid.height)
        }
    }

    @Test
    fun theSameGraphAlwaysLandsInTheSamePlace() {
        // Spatial memory only helps if the map does not rearrange itself each visit.
        val a = ForceLayout.layout(ring(20))
        val b = ForceLayout.layout(ring(20))
        a.nodes.zip(b.nodes).forEach { (x, y) ->
            assertEquals(x.x, y.x, 1e-4f)
            assertEquals(x.y, y.y, 1e-4f)
        }
    }

    @Test
    fun nodesDoNotPileUpOnTopOfEachOther() {
        val laid = ForceLayout.layout(ring(30))
        var closest = Float.MAX_VALUE
        for (i in laid.nodes.indices) {
            for (j in i + 1 until laid.nodes.size) {
                closest = minOf(closest, hypot(laid.nodes[i].x - laid.nodes[j].x, laid.nodes[i].y - laid.nodes[j].y))
            }
        }
        assertTrue("nodes overlapped (closest pair $closest)", closest > 5f)
    }

    @Test
    fun linkedNodesEndUpCloserThanUnlinkedOnes() {
        val nodes = (1..6).map { node(it.toLong()) }
        // Two triangles with nothing joining them.
        val edges = listOf(
            GraphEdge(1, 2, "x", "x"), GraphEdge(2, 3, "x", "x"), GraphEdge(3, 1, "x", "x"),
            GraphEdge(4, 5, "x", "x"), GraphEdge(5, 6, "x", "x"), GraphEdge(6, 4, "x", "x"),
        )
        val laid = ForceLayout.layout(GraphData(nodes, edges))
        val position = laid.nodes.associateBy { it.node.noteId }
        fun distance(a: Long, b: Long) =
            hypot(position.getValue(a).x - position.getValue(b).x, position.getValue(a).y - position.getValue(b).y)

        val within = listOf(distance(1, 2), distance(2, 3), distance(4, 5)).average()
        val across = listOf(distance(1, 4), distance(2, 5), distance(3, 6)).average()
        assertTrue("clusters did not separate: within=$within across=$across", across > within * 1.5)
    }

    @Test
    fun handlesDegenerateGraphs() {
        assertEquals(0, ForceLayout.layout(GraphData(emptyList(), emptyList())).nodes.size)
        assertEquals(1, ForceLayout.layout(GraphData(listOf(node(1)), emptyList())).nodes.size)
        // A self-link must not become an edge.
        val selfOnly = GraphData(listOf(node(1), node(2)), listOf(GraphEdge(1, 1, "x", "x")))
        assertEquals(0, ForceLayout.layout(selfOnly).edges.size)
    }

    @Test
    fun neighbourLookupWorksFromBothEnds() {
        val laid = ForceLayout.layout(ring(5))
        val neighbours = laid.neighboursOf(0)
        assertEquals(2, neighbours.size)
    }
}
