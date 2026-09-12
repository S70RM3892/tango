package com.tango.recall.ui.screens

import com.tango.recall.data.GraphData

/** Ways of cutting the connection map down to something readable. */
object GraphFilters {

    /**
     * Everything within [hops] relations of [centre], plus the links among them.
     *
     * The whole map is good for a sense of shape; this is what you want when you are
     * actually studying one item and its surroundings.
     */
    fun egoNetwork(data: GraphData, centre: Long, hops: Int = 2): GraphData {
        if (data.nodes.none { it.noteId == centre }) return data

        val adjacency = HashMap<Long, MutableList<Long>>()
        for (edge in data.edges) {
            adjacency.getOrPut(edge.from) { mutableListOf() } += edge.to
            adjacency.getOrPut(edge.to) { mutableListOf() } += edge.from
        }

        val depth = HashMap<Long, Int>()
        depth[centre] = 0
        val queue = ArrayDeque<Long>()
        queue += centre
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val next = depth.getValue(current) + 1
            if (next > hops) continue
            for (neighbour in adjacency[current].orEmpty()) {
                if (neighbour !in depth) {
                    depth[neighbour] = next
                    queue += neighbour
                }
            }
        }

        val kept = depth.keys
        return GraphData(
            nodes = data.nodes.filter { it.noteId in kept },
            edges = data.edges.filter { it.from in kept && it.to in kept },
        )
    }

    /** Notes that take part in no relation at all — the gaps in the web. */
    fun isolated(data: GraphData): GraphData {
        val linked = buildSet {
            data.edges.forEach { add(it.from); add(it.to) }
        }
        return GraphData(data.nodes.filter { it.noteId !in linked }, emptyList())
    }

    /** Notes whose title or subtitle contains [query], case-insensitively. */
    fun search(data: GraphData, query: String): List<Long> {
        val needle = query.trim().lowercase()
        if (needle.isEmpty()) return emptyList()
        return data.nodes
            .filter { it.title.lowercase().contains(needle) || it.subtitle.lowercase().contains(needle) }
            .map { it.noteId }
    }
}
