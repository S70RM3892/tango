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

    /**
     * @param width,height the box to lay out into. Pass the viewport's aspect ratio so
     *   a portrait screen is filled rather than letterboxed around a square drawing.
     */
    fun layout(data: GraphData, width: Float = 1000f, height: Float = 1000f): LaidOutGraph {
        val nodes = data.nodes
        val n = nodes.size
        if (n == 0) return LaidOutGraph(emptyList(), emptyList(), width, height)
        if (n == 1) {
            return LaidOutGraph(
                listOf(PositionedNode(nodes[0], width / 2, height / 2)), emptyList(), width, height,
            )
        }

        val indexOf = nodes.withIndex().associate { (i, node) -> node.noteId to i }
        val edges = data.edges.mapNotNull { edge ->
            val a = indexOf[edge.from] ?: return@mapNotNull null
            val b = indexOf[edge.to] ?: return@mapNotNull null
            if (a == b) null else PositionedEdge(a, b, edge.typeId, edge.label)
        }

        // Nothing to pull anything together: a force layout would just produce a
        // shapeless blob, so lay them out as a readable grid instead.
        if (edges.isEmpty()) return LaidOutGraph(grid(nodes, width, height), emptyList(), width, height)

        val x = FloatArray(n)
        val y = FloatArray(n)
        // Deterministic start: a ring, jittered by a seeded RNG so symmetric graphs
        // still break apart instead of collapsing onto themselves.
        val random = Random(SEED)
        for (i in 0 until n) {
            val angle = 2.0 * Math.PI * i / n
            val spread = 0.35f * (0.75f + random.nextFloat() * 0.5f)
            x[i] = (width / 2 + width * spread * cos(angle)).toFloat()
            y[i] = (height / 2 + height * spread * sin(angle)).toFloat()
        }

        val area = width * height
        val k = sqrt(area / n)
        val cutoff = k * REPULSION_CUTOFF
        val iterations = when {
            n <= 60 -> 400
            n <= 150 -> 300
            n <= 300 -> 180
            else -> 110
        }
        val longest = maxOf(width, height)
        var temperature = longest / 8f

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
                    // Without a cutoff, every disconnected cluster shoves every other
                    // one away until the drawing is enormous; rescaling it back down
                    // then crushes the local structure into unreadable dots.
                    if (distance > cutoff) continue
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
                // Pull harder along the short axis, so the cloud settles into the
                // shape of the screen instead of a circle inside it.
                dx[i] += (width / 2 - x[i]) * GRAVITY * (longest / width)
                dy[i] += (height / 2 - y[i]) * GRAVITY * (longest / height)

                val displacement = sqrt(dx[i] * dx[i] + dy[i] * dy[i])
                if (displacement > 0.001f) {
                    val step = min(displacement, temperature)
                    x[i] += dx[i] / displacement * step
                    y[i] += dy[i] / displacement * step
                }
            }
            temperature *= COOLING
        }

        normaliseInPlace(x, y, width, height)
        // Separation has to happen in final coordinates: doing it before the rescale
        // means the rescale shrinks the gaps straight back out again. A crowded map
        // needs more passes to settle, and the clamp at the edges undoes a little of
        // the work, so it is separated again after being brought inside.
        val minimumGap = sqrt(width * height / n) * MIN_GAP
        val passes = if (n > 300) 140 else 40
        separate(x, y, n, minimumGap, passes)
        clampInPlace(x, y, n, width, height)
        if (n > 300) {
            separate(x, y, n, minimumGap, passes = 40)
            clampInPlace(x, y, n, width, height)
        }

        return LaidOutGraph(
            nodes.mapIndexed { i, node -> PositionedNode(node, x[i], y[i]) },
            edges, width, height,
        )
    }

    /** Rescale into a square box with a margin, keeping the aspect ratio. */
    /** Rescale the raw force-layout coordinates into the target box. */
    private fun normaliseInPlace(x: FloatArray, y: FloatArray, width: Float, height: Float) {
        val minX = x.min(); val maxX = x.max()
        val minY = y.min(); val maxY = y.max()
        val spanX = (maxX - minX).takeIf { abs(it) > 0.01f } ?: 1f
        val spanY = (maxY - minY).takeIf { abs(it) > 0.01f } ?: 1f
        val marginX = width * MARGIN
        val marginY = height * MARGIN
        val fitX = (width - 2 * marginX) / spanX
        val fitY = (height - 2 * marginY) / spanY
        // Mostly one scale for both axes, because stretching distorts the clusters. But
        // a large graph settles into a disc, and a disc on a tall screen leaves the top
        // and bottom empty, so each axis may be stretched up to STRETCH times the
        // common scale to reach the edges. Past that, the shape matters more.
        val common = minOf(fitX, fitY)
        val scaleX = minOf(fitX, common * STRETCH)
        val scaleY = minOf(fitY, common * STRETCH)
        val offsetX = (width - spanX * scaleX) / 2
        val offsetY = (height - spanY * scaleY) / 2
        for (i in x.indices) {
            x[i] = offsetX + (x[i] - minX) * scaleX
            y[i] = offsetY + (y[i] - minY) * scaleY
        }
    }

    private fun clampInPlace(x: FloatArray, y: FloatArray, n: Int, width: Float, height: Float) {
        val marginX = width * EDGE_MARGIN
        val marginY = height * EDGE_MARGIN
        for (i in 0 until n) {
            x[i] = x[i].coerceIn(marginX, width - marginX)
            y[i] = y[i].coerceIn(marginY, height - marginY)
        }
    }

    /**
     * Push apart any pair that ended up closer than [minimumGap].
     *
     * The force pass settles the overall shape; this guarantees the result is legible,
     * which matters more than being exactly at the energy minimum.
     */
    private fun separate(x: FloatArray, y: FloatArray, n: Int, minimumGap: Float, passes: Int = 40) {
        repeat(passes) {
            var moved = false
            for (i in 0 until n) {
                for (j in i + 1 until n) {
                    var deltaX = x[i] - x[j]
                    var deltaY = y[i] - y[j]
                    var distance = sqrt(deltaX * deltaX + deltaY * deltaY)
                    if (distance >= minimumGap) continue
                    if (distance < 0.001f) {
                        deltaX = ((i % 5) - 2).toFloat() + 0.5f
                        deltaY = ((j % 5) - 2).toFloat() + 0.5f
                        distance = sqrt(deltaX * deltaX + deltaY * deltaY)
                    }
                    val push = (minimumGap - distance) / 2f
                    val ux = deltaX / distance
                    val uy = deltaY / distance
                    x[i] += ux * push; y[i] += uy * push
                    x[j] -= ux * push; y[j] -= uy * push
                    moved = true
                }
            }
            if (!moved) return
        }
    }

    private fun grid(nodes: List<GraphNode>, width: Float, height: Float): List<PositionedNode> {
        val columns = kotlin.math.ceil(sqrt(nodes.size * width / height)).toInt().coerceAtLeast(1)
        val rows = kotlin.math.ceil(nodes.size.toFloat() / columns).toInt().coerceAtLeast(1)
        val marginX = width * MARGIN
        val marginY = height * MARGIN
        val stepX = if (columns > 1) (width - 2 * marginX) / (columns - 1) else 0f
        val stepY = if (rows > 1) (height - 2 * marginY) / (rows - 1) else 0f
        return nodes.mapIndexed { i, node ->
            PositionedNode(
                node,
                if (columns > 1) marginX + (i % columns) * stepX else width / 2,
                if (rows > 1) marginY + (i / columns) * stepY else height / 2,
            )
        }
    }

    private const val SEED = 20260912L
    private const val GRAVITY = 0.02f
    /** Repulsion range, in multiples of the ideal edge length. */
    private const val REPULSION_CUTOFF = 3.2f
    /** Closest two notes may end up, in multiples of the mean spacing. */
    private const val MIN_GAP = 0.62f
    private const val EDGE_MARGIN = 0.035f
    private const val COOLING = 0.985f
    private const val MARGIN = 0.06f

    /** How far one axis may be stretched past the other to reach the screen edges. */
    private const val STRETCH = 1.4f
}
