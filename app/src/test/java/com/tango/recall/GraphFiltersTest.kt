package com.tango.recall

import com.tango.recall.data.GraphData
import com.tango.recall.data.GraphEdge
import com.tango.recall.data.GraphNode
import com.tango.recall.ui.screens.GraphFilters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GraphFiltersTest {

    private fun node(id: Long, title: String = "n$id", subtitle: String = "") = GraphNode(
        noteId = id, deckId = 1, typeId = "english",
        title = title, subtitle = subtitle, degree = 0, strength = 0.5, isNew = false,
    )

    /** 1 — 2 — 3 — 4, plus an unattached 9. */
    private val chain = GraphData(
        nodes = listOf(node(1), node(2), node(3), node(4), node(9)),
        edges = listOf(
            GraphEdge(1, 2, "x", "x"),
            GraphEdge(2, 3, "x", "x"),
            GraphEdge(3, 4, "x", "x"),
        ),
    )

    @Test
    fun egoNetworkReachesExactlyAsFarAsAsked() {
        assertEquals(setOf(1L, 2L), GraphFilters.egoNetwork(chain, 1, hops = 1).nodes.map { it.noteId }.toSet())
        assertEquals(setOf(1L, 2L, 3L), GraphFilters.egoNetwork(chain, 1, hops = 2).nodes.map { it.noteId }.toSet())
        assertEquals(
            setOf(1L, 2L, 3L, 4L),
            GraphFilters.egoNetwork(chain, 1, hops = 5).nodes.map { it.noteId }.toSet(),
        )
    }

    @Test
    fun egoNetworkKeepsOnlyTheEdgesBetweenSurvivingNodes() {
        val ego = GraphFilters.egoNetwork(chain, 1, hops = 1)
        assertEquals(1, ego.edges.size)
        assertTrue(ego.edges.all { it.from in setOf(1L, 2L) && it.to in setOf(1L, 2L) })
    }

    @Test
    fun egoNetworkTravelsInBothDirections() {
        // 4 is only ever an edge target, so a one-way walk would miss its neighbours.
        assertEquals(setOf(3L, 4L), GraphFilters.egoNetwork(chain, 4, hops = 1).nodes.map { it.noteId }.toSet())
    }

    @Test
    fun anIsolatedCentreKeepsOnlyItself() {
        assertEquals(listOf(9L), GraphFilters.egoNetwork(chain, 9, hops = 3).nodes.map { it.noteId })
    }

    @Test
    fun anUnknownCentreLeavesTheGraphAlone() {
        assertEquals(chain.nodes.size, GraphFilters.egoNetwork(chain, 999).nodes.size)
    }

    @Test
    fun isolatedFindsTheNotesWithNoRelations() {
        val only = GraphFilters.isolated(chain)
        assertEquals(listOf(9L), only.nodes.map { it.noteId })
        assertTrue(only.edges.isEmpty())
    }

    @Test
    fun searchMatchesTitleAndSubtitleIgnoringCase() {
        val data = GraphData(
            nodes = listOf(
                node(1, "Perspective", "観点"),
                node(2, "conspicuous", "目立つ"),
                node(3, "硫酸", "H2SO4"),
            ),
            edges = emptyList(),
        )
        assertEquals(listOf(1L), GraphFilters.search(data, "perspec"))
        assertEquals(listOf(2L), GraphFilters.search(data, "目立"))
        assertEquals(listOf(3L), GraphFilters.search(data, "h2so4"))
        assertTrue(GraphFilters.search(data, "   ").isEmpty())
        assertTrue(GraphFilters.search(data, "存在しない").isEmpty())
    }
}
