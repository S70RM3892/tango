package com.tango.recall.ui.screens

import com.tango.recall.data.GraphData
import com.tango.recall.data.GraphNode
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

data class PositionedNode(val node: GraphNode, val x: Float, val y: Float)

data class PositionedEdge(
    val fromIndex: Int,
    val toIndex: Int,
    val typeId: String,
    val label: String,
)

data class LaidOutGraph(
    val nodes: List<PositionedNode>,
    val edges: List<PositionedEdge>,
    val width: Float,
    val height: Float,
) {
    fun neighboursOf(index: Int): List<Int> =
        edges.mapNotNull {
            when (index) {
                it.fromIndex -> it.toIndex
                it.toIndex -> it.fromIndex
                else -> null
            }
        }
}

/**
 * Fruchterman–Reingold force-directed layout.
 *
 * Nodes repel each other, linked nodes pull together, and a weak pull toward the
 * centre keeps disconnected clusters from drifting apart forever. The starting
 * positions come from a fixed seed, so the same notes land in the same place every
 * time — the map is only useful as a memory aid if it stops moving.
 */
object ForceLayout {

    fun layout(data: GraphData, size: Float = 1000f): LaidOutGraph {
        val nodes = data.nodes
        val n = nodes.size
        if (n == 0) return LaidOutGraph(emptyList(), emptyList(), size, size)
        if (n == 1) return LaidOutGraph(listOf(PositionedNode(nodes[0], size / 2, size / 2)), emptyList(), size, size)

        val indexOf = nodes.withIndex().associate { (i, node) -> node.noteId to i }
        val edges = data.edges.mapNotNull { edge ->
            val a = indexOf[edge.from] ?: return@mapNotNull null
            val b = indexOf[edge.to] ?: return@mapNotNull null
            if (a == b) null else PositionedEdge(a, b, edge.typeId, edge.label)
        }

        val x = FloatArray(n)
        val y = FloatArray(n)
        // Deterministic start: a ring, jittered by a seeded RNG so symmetric graphs
        // still break apart instead of collapsing onto themselves.
        val random = Random(SEED)
        for (i in 0 until n) {
            val angle = 2.0 * Math.PI * i / n
            val radius = size * 0.35f * (0.75f + random.nextFloat() * 0.5f)
            x[i] = (size / 2 + radius * cos(angle)).toFloat()
            y[i] = (size / 2 + radius * sin(angle)).toFloat()
        }

        val area = size * size
        val k = sqrt(area / n)
        val iterations = when {
            n <= 60 -> 400
            n <= 150 -> 300
            n <= 300 -> 180
            else -> 110
        }
        var temperature = size / 8f

        val dx = FloatArray(n)
        val dy = FloatArray(n)

        repeat(iterations) {
            java.util.Arrays.fill(dx, 0f)
            java.util.Arrays.fill(dy, 0f)

            for (i in 0 until n) {
                for (j in i + 1 until n) {
                    var deltaX = x[i] - x[j]
                    var deltaY = y[i] - y[j]
                    var distance = sqrt(deltaX * deltaX + deltaY * deltaY)
                    if (distance < 0.01f) {
                        // Two nodes exactly on top of each other have no direction to
                        // push apart in; nudge them deterministically.
                        deltaX = ((i % 7) - 3).toFloat(); deltaY = ((j % 7) - 3).toFloat()
                        distance = sqrt(deltaX * deltaX + deltaY * deltaY).coerceAtLeast(0.01f)
                    }
                    val repulsion = k * k / distance
                    val ux = deltaX / distance
                    val uy = deltaY / distance
                    dx[i] += ux * repulsion; dy[i] += uy * repulsion
                    dx[j] -= ux * repulsion; dy[j] -= uy * repulsion
                }
            }

            for (edge in edges) {
                val a = edge.fromIndex
                val b = edge.toIndex
                val deltaX = x[a] - x[b]
                val deltaY = y[a] - y[b]
                val distance = sqrt(deltaX * deltaX + deltaY * deltaY).coerceAtLeast(0.01f)
                val attraction = distance * distance / k
                val ux = deltaX / distance
                val uy = deltaY / distance
                dx[a] -= ux * attraction; dy[a] -= uy * attraction
                dx[b] += ux * attraction; dy[b] += uy * attraction
            }

            for (i in 0 until n) {
                dx[i] += (size / 2 - x[i]) * GRAVITY
                dy[i] += (size / 2 - y[i]) * GRAVITY

                val displacement = sqrt(dx[i] * dx[i] + dy[i] * dy[i])
                if (displacement > 0.001f) {
                    val step = min(displacement, temperature)
                    x[i] += dx[i] / displacement * step
                    y[i] += dy[i] / displacement * step
                }
            }
            temperature *= COOLING
        }

        return normalise(nodes, x, y, size)
            .let { LaidOutGraph(it, edges, size, size) }
    }

    /** Rescale into a square box with a margin, keeping the aspect ratio. */
    private fun normalise(nodes: List<GraphNode>, x: FloatArray, y: FloatArray, size: Float): List<PositionedNode> {
        val minX = x.min(); val maxX = x.max()
        val minY = y.min(); val maxY = y.max()
        val spanX = (maxX - minX).takeIf { abs(it) > 0.01f } ?: 1f
        val spanY = (maxY - minY).takeIf { abs(it) > 0.01f } ?: 1f
        val span = maxOf(spanX, spanY)
        val margin = size * MARGIN
        val scale = (size - 2 * margin) / span
        val offsetX = margin + (size - 2 * margin - spanX * scale) / 2
        val offsetY = margin + (size - 2 * margin - spanY * scale) / 2

        return nodes.mapIndexed { i, node ->
            PositionedNode(node, offsetX + (x[i] - minX) * scale, offsetY + (y[i] - minY) * scale)
        }
    }

    private const val SEED = 20260912L
    private const val GRAVITY = 0.012f
    private const val COOLING = 0.985f
    private const val MARGIN = 0.08f
}
