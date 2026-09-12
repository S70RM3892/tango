package com.tango.recall.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tango.recall.Routes
import com.tango.recall.data.NoteType
import com.tango.recall.ui.AppViewModel
import kotlin.math.hypot
import kotlin.math.sqrt

/**
 * The connection map.
 *
 * Notes are drawn as cell bodies and relations as axons, with a pulse running along
 * the links of whatever is selected. Brightness is not decoration: it is the
 * predicted recall of that note, so a dim patch of the map is literally the part of
 * the material that is fading.
 */
@Composable
fun GraphScreen(vm: AppViewModel, nav: NavController, initialDeckId: Long?) {
    var deckId by remember { mutableStateOf(initialDeckId) }
    var graph by remember { mutableStateOf<LaidOutGraph?>(null) }
    var loading by remember { mutableStateOf(true) }
    var isolatedOnly by remember { mutableStateOf(false) }

    var selected by remember { mutableStateOf<Int?>(null) }
    var zoom by remember { mutableStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(deckId, isolatedOnly) {
        loading = true
        selected = null
        val data = vm.graph(deckId)
        val filtered = if (!isolatedOnly) data else {
            val linked = data.edges.flatMap { listOf(it.from, it.to) }.toSet()
            data.copy(nodes = data.nodes.filter { it.noteId !in linked }, edges = emptyList())
        }
        graph = ForceLayout.layout(filtered)
        zoom = 1f
        pan = Offset.Zero
        loading = false
    }

    Scaffold(
        topBar = {
            TangoTopBar("つながり地図", onBack = { nav.popBackStack() })
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = deckId == null && !isolatedOnly,
                    onClick = { deckId = null; isolatedOnly = false },
                    label = { Text("すべて") },
                )
                vm.decks.forEach { deck ->
                    FilterChip(
                        selected = deckId == deck.id && !isolatedOnly,
                        onClick = { deckId = deck.id; isolatedOnly = false },
                        label = { Text(deck.name) },
                    )
                }
                FilterChip(
                    selected = isolatedOnly,
                    onClick = { isolatedOnly = !isolatedOnly },
                    label = { Text("孤立しているノート") },
                )
            }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                val laid = graph
                when {
                    loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }

                    laid == null || laid.nodes.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Text(
                            if (isolatedOnly) "孤立しているノートはありません。よくつながっています。"
                            else "表示できるノートがありません。",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    else -> GraphCanvas(
                        graph = laid,
                        selected = selected,
                        zoom = zoom,
                        pan = pan,
                        onTransform = { newPan, newZoom -> pan = newPan; zoom = newZoom },
                        onSelect = { selected = it },
                    )
                }

                selected?.let { index ->
                    laid?.let { g ->
                        NodePanel(
                            graph = g,
                            index = index,
                            modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp),
                            onOpen = {
                                val node = g.nodes[index].node
                                nav.navigate(Routes.note(node.noteId, node.deckId))
                            },
                            onDismiss = { selected = null },
                        )
                    }
                }
            }

            Legend(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp))
        }
    }
}

@Composable
private fun GraphCanvas(
    graph: LaidOutGraph,
    selected: Int?,
    zoom: Float,
    pan: Offset,
    onTransform: (Offset, Float) -> Unit,
    onSelect: (Int?) -> Unit,
) {
    val measurer = rememberTextMeasurer()
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing)),
        label = "pulse",
    )

    // Screen position is computed by hand rather than with a canvas transform, so
    // line widths and labels stay crisp at any zoom.
    var canvasSize by remember { mutableStateOf(Offset.Zero) }
    fun baseScale(): Float =
        if (canvasSize == Offset.Zero) 1f
        else minOf(canvasSize.x / graph.width, canvasSize.y / graph.height)

    fun toScreen(x: Float, y: Float): Offset {
        val s = baseScale() * zoom
        val cx = canvasSize.x / 2
        val cy = canvasSize.y / 2
        return Offset(
            cx + (x - graph.width / 2) * s + pan.x,
            cy + (y - graph.height / 2) * s + pan.y,
        )
    }

    val neighbours = remember(graph, selected) {
        selected?.let { graph.neighboursOf(it).toSet() } ?: emptySet()
    }

    Canvas(
        Modifier
            .fillMaxSize()
            .onSizeChanged { canvasSize = Offset(it.width.toFloat(), it.height.toFloat()) }
            .pointerInput(graph) {
                detectTransformGestures { _, panChange, zoomChange, _ ->
                    onTransform(pan + panChange, (zoom * zoomChange).coerceIn(0.4f, 6f))
                }
            }
            .pointerInput(graph, zoom, pan) {
                detectTapGestures { tap ->
                    val hit = graph.nodes.indices.minByOrNull { i ->
                        val p = toScreen(graph.nodes[i].x, graph.nodes[i].y)
                        hypot(p.x - tap.x, p.y - tap.y)
                    }
                    val within = hit?.let {
                        val p = toScreen(graph.nodes[it].x, graph.nodes[it].y)
                        hypot(p.x - tap.x, p.y - tap.y) < 48f
                    } ?: false
                    onSelect(if (within) hit else null)
                }
            },
    ) {
        drawRect(FIELD_BACKGROUND)

        val animateAll = graph.edges.size <= 140
        // A radial gradient per node is the costly part of a frame; on a large map
        // keep the glow for the nodes that carry meaning right now.
        val glowEverything = graph.nodes.size <= 200

        // --- axons -----------------------------------------------------------
        for (edge in graph.edges) {
            val a = graph.nodes[edge.fromIndex]
            val b = graph.nodes[edge.toIndex]
            val p0 = toScreen(a.x, a.y)
            val p1 = toScreen(b.x, b.y)
            val highlighted = selected != null &&
                (edge.fromIndex == selected || edge.toIndex == selected)
            if (selected != null && !highlighted) {
                drawAxon(p0, p1, colorFor(a.node.typeId).copy(alpha = 0.10f), 1f, null, 0f)
                continue
            }
            val tint = colorFor(a.node.typeId)
            val alpha = if (highlighted) 0.85f else 0.30f
            val width = if (highlighted) 2.4f else 1.3f
            val travelling = if (highlighted || animateAll) pulse else null
            drawAxon(p0, p1, tint.copy(alpha = alpha), width, travelling, if (highlighted) 1f else 0.45f)
        }

        // --- cell bodies -----------------------------------------------------
        for ((index, positioned) in graph.nodes.withIndex()) {
            val node = positioned.node
            val centre = toScreen(positioned.x, positioned.y)
            if (centre.x < -120f || centre.y < -120f ||
                centre.x > size.width + 120f || centre.y > size.height + 120f
            ) continue

            val radius = (7f + 3.2f * sqrt(node.degree.toFloat())).coerceAtMost(26f) * zoom.coerceIn(0.7f, 1.6f)
            val tint = colorFor(node.typeId)
            val isSelected = index == selected
            val isNeighbour = index in neighbours
            // A note never studied is drawn as an empty outline; one that is fading
            // loses its glow before it loses its outline.
            val vitality = if (node.isNew) 0.0f else (0.25f + 0.75f * node.strength.toFloat())
            val dimmed = selected != null && !isSelected && !isNeighbour

            if (glowEverything || isSelected || isNeighbour || node.degree >= 4) {
                val glowAlpha = (if (dimmed) 0.06f else 0.30f) * (0.35f + vitality)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(tint.copy(alpha = glowAlpha), Color.Transparent),
                        center = centre,
                        radius = radius * 3.4f,
                    ),
                    radius = radius * 3.4f,
                    center = centre,
                )
            }

            drawDendrites(centre, radius, tint.copy(alpha = if (dimmed) 0.08f else 0.22f), index)

            if (node.isNew) {
                drawCircle(
                    color = tint.copy(alpha = if (dimmed) 0.2f else 0.55f),
                    radius = radius,
                    center = centre,
                    style = Stroke(width = 1.6f),
                )
            } else {
                drawCircle(
                    color = tint.copy(alpha = if (dimmed) 0.18f else 0.35f + 0.45f * vitality),
                    radius = radius,
                    center = centre,
                )
                drawCircle(
                    color = Color.White.copy(alpha = if (dimmed) 0.06f else 0.18f + 0.5f * vitality),
                    radius = radius * 0.42f,
                    center = centre,
                )
            }

            if (isSelected) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.9f),
                    radius = radius + 5f,
                    center = centre,
                    style = Stroke(width = 2f),
                )
            }

            val showLabel = isSelected || isNeighbour ||
                (selected == null && (zoom > 1.35f || node.degree >= 4))
            if (showLabel) {
                drawLabel(measurer, node.title, centre, radius, dimmed)
            }
        }
    }
}

/** A slightly curved, tapered connection, optionally carrying a travelling signal. */
private fun DrawScope.drawAxon(
    p0: Offset,
    p1: Offset,
    color: Color,
    width: Float,
    travel: Float?,
    signalStrength: Float,
) {
    val midX = (p0.x + p1.x) / 2
    val midY = (p0.y + p1.y) / 2
    val dx = p1.x - p0.x
    val dy = p1.y - p0.y
    val length = hypot(dx, dy)
    if (length < 0.5f) return
    // Bow the line out perpendicular to itself so parallel links stay distinguishable.
    val control = Offset(midX - dy * CURVATURE, midY + dx * CURVATURE)

    val path = Path().apply {
        moveTo(p0.x, p0.y)
        quadraticTo(control.x, control.y, p1.x, p1.y)
    }
    drawPath(path, color, style = Stroke(width = width))

    if (travel != null && signalStrength > 0f) {
        val t = travel
        val inv = 1 - t
        val sx = inv * inv * p0.x + 2 * inv * t * control.x + t * t * p1.x
        val sy = inv * inv * p0.y + 2 * inv * t * control.y + t * t * p1.y
        val centre = Offset(sx, sy)
        // Fade the signal in and out so it does not pop at the ends.
        val fade = (1f - kotlin.math.abs(0.5f - t) * 2f).coerceIn(0f, 1f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.85f * fade * signalStrength),
                    color.copy(alpha = 0f),
                ),
                center = centre,
                radius = 9f,
            ),
            radius = 9f,
            center = centre,
        )
    }
}

/** Short faint spurs that read as dendrites. Angles are fixed per node. */
private fun DrawScope.drawDendrites(centre: Offset, radius: Float, color: Color, seed: Int) {
    val count = 4
    for (i in 0 until count) {
        val angle = ((seed * 47 + i * 360 / count) % 360) * Math.PI / 180.0
        val start = Offset(
            centre.x + (radius * 0.95f * kotlin.math.cos(angle)).toFloat(),
            centre.y + (radius * 0.95f * kotlin.math.sin(angle)).toFloat(),
        )
        val end = Offset(
            centre.x + (radius * 1.9f * kotlin.math.cos(angle)).toFloat(),
            centre.y + (radius * 1.9f * kotlin.math.sin(angle)).toFloat(),
        )
        drawLine(color, start, end, strokeWidth = 1.2f)
    }
}

private fun DrawScope.drawLabel(
    measurer: TextMeasurer,
    text: String,
    centre: Offset,
    radius: Float,
    dimmed: Boolean,
) {
    val trimmed = if (text.length > 18) text.take(17) + "…" else text
    val layout = measurer.measure(
        trimmed,
        TextStyle(
            color = Color.White.copy(alpha = if (dimmed) 0.25f else 0.92f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        ),
    )
    drawText(
        layout,
        topLeft = Offset(centre.x - layout.size.width / 2f, centre.y + radius + 6f),
    )
}

@Composable
private fun NodePanel(
    graph: LaidOutGraph,
    index: Int,
    modifier: Modifier = Modifier,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
) {
    val node = graph.nodes[index].node
    val connections = graph.edges.mapNotNull { edge ->
        when (index) {
            edge.fromIndex -> edge.label to graph.nodes[edge.toIndex].node.title
            edge.toIndex -> edge.label to graph.nodes[edge.fromIndex].node.title
            else -> null
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(node.title, style = MaterialTheme.typography.titleMedium)
            if (node.subtitle.isNotBlank()) {
                Text(
                    node.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Pill("つながり ${node.degree}")
                Pill(
                    if (node.isNew) "未学習"
                    else "思い出しやすさ ${(node.strength * 100).toInt()}%",
                )
            }
            if (connections.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    connections.take(6).forEach { (label, title) ->
                        Text(
                            "$label → $title",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (connections.size > 6) {
                        Text(
                            "ほか ${connections.size - 6} 件",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onOpen, modifier = Modifier.weight(1f)) { Text("ノートを開く") }
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("閉じる") }
            }
        }
    }
}

@Composable
private fun Legend(modifier: Modifier = Modifier) {
    Row(
        modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        NoteType.entries.forEach { type ->
            Pill(type.label, colorFor(type.id).copy(alpha = 0.22f), MaterialTheme.colorScheme.onSurface)
        }
    }
}

private val FIELD_BACKGROUND = Color(0xFF0A0E1A)
private const val CURVATURE = 0.10f

/** One hue per note type, chosen to stay legible on the dark field. */
private fun colorFor(typeId: String): Color = when (typeId) {
    NoteType.ENGLISH.id -> Color(0xFF5B9BFF)
    NoteType.CHEM_SUBSTANCE.id -> Color(0xFF2DD4BF)
    NoteType.CHEM_REACTION.id -> Color(0xFFFBBF24)
    NoteType.EISAKUBUN.id -> Color(0xFFC084FC)
    NoteType.CHEM_CALC.id -> Color(0xFF4ADE80)
    else -> Color(0xFF94A3B8)
}
