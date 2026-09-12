package com.tango.recall.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tango.recall.Routes
import com.tango.recall.data.GraphData
import com.tango.recall.data.NoteType
import com.tango.recall.ui.AppViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private const val MIN_SCALE = 0.25f
private const val MAX_SCALE = 6f
private val FIELD_BACKGROUND = Color(0xFF0A0E1A)
private const val CURVATURE = 0.10f

/** What the map is currently showing. */
private enum class MapMode { ALL, ISOLATED, FOCUS }

/**
 * The connection map.
 *
 * Notes are cell bodies, relations are axons, and the links of whatever is selected
 * carry a travelling signal. Brightness is not decoration: it is the predicted recall
 * of that note, so a dim patch of the map is the part of the material that is fading.
 */
@Composable
fun GraphScreen(vm: AppViewModel, nav: NavController, initialDeckId: Long?) {
    var deckId by remember { mutableStateOf(initialDeckId) }
    var mode by remember { mutableStateOf(MapMode.ALL) }
    var focusId by remember { mutableStateOf<Long?>(null) }

    var source by remember { mutableStateOf<GraphData?>(null) }
    var laid by remember { mutableStateOf<LaidOutGraph?>(null) }
    var loading by remember { mutableStateOf(true) }

    var viewport by remember { mutableStateOf(Size.Zero) }
    var horizonIndex by remember { mutableStateOf(0f) }
    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var centreOn by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(deckId) {
        loading = true
        source = vm.graph(deckId)
    }

    // Quantised so a one-pixel resize does not trigger a fresh layout.
    val shapeKey = if (viewport == Size.Zero) 0 else ((viewport.width / viewport.height) * 20).toInt()

    LaunchedEffect(source, mode, focusId, shapeKey) {
        val data = source ?: return@LaunchedEffect
        if (viewport == Size.Zero) return@LaunchedEffect
        loading = true
        val filtered = when (mode) {
            MapMode.ALL -> data
            MapMode.ISOLATED -> GraphFilters.isolated(data)
            MapMode.FOCUS -> focusId?.let { GraphFilters.egoNetwork(data, it, hops = 2) } ?: data
        }
        // Lay out into the screen's own shape; a square drawing on a tall phone wastes
        // the top and bottom of the display.
        //
        // Off the main thread: the layout is O(nodes²) per iteration, so running it in
        // the composition's own context froze the UI for as long as it took — a second
        // or more on a full map, which is where the map felt broken on first open.
        laid = withContext(Dispatchers.Default) {
            ForceLayout.layout(
                filtered,
                width = 1000f * (viewport.width / viewport.height),
                height = 1000f,
            )
        }
        loading = false
    }

    val matches = remember(source, query) {
        source?.let { GraphFilters.search(it, query) }.orEmpty()
    }

    Scaffold(
        topBar = {
            TangoTopBar("つながり地図", onBack = { nav.popBackStack() }) {
                IconButton(onClick = { searching = !searching; if (!searching) query = "" }) {
                    Icon(
                        if (searching) Icons.Default.Clear else Icons.Default.Search,
                        contentDescription = if (searching) "検索を閉じる" else "検索",
                    )
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            if (searching) {
                SearchPanel(
                    query = query,
                    onQueryChange = { query = it },
                    results = matches.take(6).mapNotNull { id ->
                        source?.nodes?.firstOrNull { it.noteId == id }?.let { id to it.title }
                    },
                    onPick = { id ->
                        centreOn = id
                        searching = false
                        query = ""
                    },
                )
            }

            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = mode == MapMode.ALL && deckId == null,
                    onClick = { mode = MapMode.ALL; focusId = null; deckId = null },
                    label = { Text("すべて") },
                )
                vm.decks.forEach { deck ->
                    FilterChip(
                        selected = mode == MapMode.ALL && deckId == deck.id,
                        onClick = { mode = MapMode.ALL; focusId = null; deckId = deck.id },
                        label = { Text(deck.name) },
                    )
                }
                FilterChip(
                    selected = mode == MapMode.ISOLATED,
                    onClick = { mode = if (mode == MapMode.ISOLATED) MapMode.ALL else MapMode.ISOLATED },
                    label = { Text("孤立しているノート") },
                )
                if (mode == MapMode.FOCUS) {
                    FilterChip(
                        selected = true,
                        onClick = { mode = MapMode.ALL; focusId = null },
                        label = { Text("周辺だけ表示中 ✕") },
                    )
                }
            }

            // Pinned once per visit: reading the clock during composition would make
            // the horizon a new value on every frame and recompose the canvas with it.
            val openedAt = remember { System.currentTimeMillis() }
            val examDays = remember(vm.examDate, openedAt) {
                vm.examDate.takeIf { it > 0 }
                    ?.let { ((it - openedAt) / 86_400_000L).toInt() }
                    ?.takeIf { it > 0 }
            }
            val stops = remember(examDays) { horizonStops(examDays) }
            val horizonDays = stops[horizonIndex.roundToInt().coerceIn(stops.indices)]
            val atTime = openedAt + horizonDays * 86_400_000L

            Box(
                Modifier.weight(1f).fillMaxWidth()
                    .onSizeChanged { viewport = Size(it.width.toFloat(), it.height.toFloat()) },
            ) {
                val graph = laid
                when {
                    loading || graph == null -> Box(Modifier.fillMaxSize().background(FIELD_BACKGROUND), Alignment.Center) {
                        CircularProgressIndicator()
                    }

                    graph.nodes.isEmpty() -> Box(
                        Modifier.fillMaxSize().background(FIELD_BACKGROUND),
                        Alignment.Center,
                    ) {
                        Text(
                            when (mode) {
                                MapMode.ISOLATED -> "孤立しているノートはありません。よくつながっています。"
                                else -> "表示できるノートがありません。"
                            },
                            color = Color.White.copy(alpha = 0.6f),
                        )
                    }

                    else -> GraphView(
                        graph = graph,
                        viewport = viewport,
                        atTime = atTime,
                        centreOn = centreOn,
                        onCentred = { centreOn = null },
                        onOpenNote = { node -> nav.navigate(Routes.note(node.noteId, node.deckId)) },
                        onFocus = { noteId -> focusId = noteId; mode = MapMode.FOCUS },
                    )
                }
            }

            // Dragging the slider recomposes on every frame; the average is a
            // forgetting curve per card, so it is worked out once per stop.
            val meanStrength = remember(laid, atTime) {
                laid?.nodes
                    ?.filterNot { it.node.isNew }
                    ?.map { it.node.strengthAt(atTime) }
                    ?.takeIf { it.isNotEmpty() }?.average()
            }

            TimeSlider(
                stops = stops,
                index = horizonIndex,
                onIndexChange = { horizonIndex = it },
                horizonDays = horizonDays,
                examDays = examDays,
                meanStrength = meanStrength,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )

            laid?.let { Legend(it, Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) }
        }
    }
}

@Composable
private fun SearchPanel(
    query: String,
    onQueryChange: (String) -> Unit,
    results: List<Pair<Long, String>>,
    onPick: (Long) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            label = { Text("地図の中を探す") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        results.forEach { (id, title) ->
            Text(
                title,
                modifier = Modifier.fillMaxWidth().clickable { onPick(id) }.padding(vertical = 10.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        if (query.isNotBlank() && results.isEmpty()) {
            Text(
                "見つかりませんでした。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun GraphView(
    graph: LaidOutGraph,
    viewport: Size,
    atTime: Long,
    centreOn: Long?,
    onCentred: () -> Unit,
    onOpenNote: (com.tango.recall.data.GraphNode) -> Unit,
    onFocus: (Long) -> Unit,
) {
    // One entry per label that can be on screen at once. The default cache holds 8,
    // so with the signal animation redrawing continuously every label was being laid
    // out again on every single frame.
    val measurer = rememberTextMeasurer(cacheSize = MAX_LABELS + 8)

    // Everything the gesture handlers touch is state read through a delegate, never a
    // captured parameter — that is what broke the pinch the first time round.
    var camera by remember(graph) { mutableStateOf(GraphCamera.Identity) }
    var selected by remember(graph) { mutableStateOf<Int?>(null) }
    var dragging by remember(graph) { mutableStateOf<Int?>(null) }
    var fitRequest by remember { mutableStateOf(0) }

    val positions = remember(graph) {
        mutableStateListOf<Offset>().apply { addAll(graph.nodes.map { Offset(it.x, it.y) }) }
    }

    // How brightly each node burns. It is a forgetting curve per card, and it only
    // changes when the time slider moves — not on every frame of the animation.
    val vitalities = remember(graph, atTime) {
        FloatArray(graph.nodes.size) { i ->
            val node = graph.nodes[i].node
            if (node.isNew) 0f else (0.25f + 0.75f * node.strengthAt(atTime).toFloat())
        }
    }

    val appear by animateFloatAsState(
        targetValue = if (viewport == Size.Zero) 0f else 1f,
        animationSpec = tween(450),
        label = "appear",
    )
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing)),
        label = "pulse",
    )

    LaunchedEffect(graph, viewport, fitRequest) {
        if (viewport != Size.Zero) {
            camera = GraphCamera.fit(Size(graph.width, graph.height), viewport, padding = 56f)
        }
    }

    LaunchedEffect(centreOn, viewport, graph) {
        val target = centreOn ?: return@LaunchedEffect
        if (viewport == Size.Zero) return@LaunchedEffect
        val index = graph.nodes.indexOfFirst { it.node.noteId == target }
        if (index >= 0) {
            camera = camera.copy(scale = camera.scale.coerceAtLeast(1f)).centredOn(positions[index], viewport)
            selected = index
        }
        onCentred()
    }

    fun radiusOf(index: Int): Float {
        val degree = graph.nodes[index].node.degree
        return ((8f + 3.4f * sqrt(degree.toFloat())) * camera.scale).coerceIn(4f, 44f)
    }

    fun hitTest(point: Offset): Int? {
        var best: Int? = null
        var bestDistance = Float.MAX_VALUE
        for (i in positions.indices) {
            val screen = camera.toScreen(positions[i])
            val distance = hypot(screen.x - point.x, screen.y - point.y)
            val reach = (radiusOf(i) + 18f).coerceAtLeast(30f)
            if (distance <= reach && distance < bestDistance) {
                bestDistance = distance
                best = i
            }
        }
        return best
    }

    Box(Modifier.fillMaxSize()) {
        Canvas(
            Modifier
                .fillMaxSize()
                .background(FIELD_BACKGROUND)
                .pointerInput(graph) {
                    // No double-tap handler on purpose: waiting to see whether a second
                    // tap is coming would delay every single selection by ~300ms.
                    // Zooming is covered by the pinch gesture and the buttons.
                    detectTapGestures { selected = hitTest(it) }
                }
                .pointerInput(graph) {
                    // Long press picks a node up; a plain drag falls through to panning.
                    detectDragGesturesAfterLongPress(
                        onDragStart = { dragging = hitTest(it) },
                        onDragEnd = { dragging = null },
                        onDragCancel = { dragging = null },
                        onDrag = { _, delta ->
                            dragging?.let { positions[it] = positions[it] + delta / camera.scale }
                        },
                    )
                }
                .pointerInput(graph) {
                    detectTransformGestures { centroid, panChange, zoomChange, _ ->
                        camera = camera.panned(panChange).zoomedAround(centroid, zoomChange, MIN_SCALE, MAX_SCALE)
                    }
                },
        ) {
            val neighbours = selected?.let { graph.neighboursOf(it).toSet() }.orEmpty()
            val animateAll = graph.edges.size <= 140
            val glowEverything = graph.nodes.size <= 200

            // --- axons --------------------------------------------------------
            for (edge in graph.edges) {
                val p0 = camera.toScreen(positions[edge.fromIndex])
                val p1 = camera.toScreen(positions[edge.toIndex])
                val tint = colorFor(graph.nodes[edge.fromIndex].node.typeId)
                val highlighted = selected != null &&
                    (edge.fromIndex == selected || edge.toIndex == selected)

                if (selected != null && !highlighted) {
                    drawAxon(p0, p1, tint.copy(alpha = 0.13f * appear), 1.1f, null, 0f)
                } else {
                    val alpha = (if (highlighted) 0.9f else 0.42f) * appear
                    val width = if (highlighted) 2.6f else 1.5f
                    val travelling = if (highlighted || animateAll) pulse else null
                    drawAxon(p0, p1, tint.copy(alpha = alpha), width, travelling, if (highlighted) 1f else 0.45f)
                }
            }

            // --- cell bodies --------------------------------------------------
            for (index in graph.nodes.indices) {
                val node = graph.nodes[index].node
                val centre = camera.toScreen(positions[index])
                val radius = radiusOf(index)
                if (centre.x < -radius * 4 || centre.y < -radius * 4 ||
                    centre.x > size.width + radius * 4 || centre.y > size.height + radius * 4
                ) continue

                val tint = colorFor(node.typeId)
                val isSelected = index == selected
                val isNeighbour = index in neighbours
                val isDragged = index == dragging
                // Never studied: outline only. Fading: the glow goes before the outline.
                val vitality = vitalities[index]
                val dimmed = selected != null && !isSelected && !isNeighbour

                if (glowEverything || isSelected || isNeighbour || node.degree >= 4) {
                    val glowAlpha = (if (dimmed) 0.06f else 0.30f) * (0.35f + vitality) * appear
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

                drawDendrites(centre, radius, tint.copy(alpha = (if (dimmed) 0.08f else 0.22f) * appear), index)

                if (node.isNew) {
                    // Hollow, but not invisible: on a fresh install every note is new,
                    // and a map of faint rings looks broken rather than untouched.
                    drawCircle(
                        color = tint.copy(alpha = (if (dimmed) 0.06f else 0.16f) * appear),
                        radius = radius,
                        center = centre,
                    )
                    drawCircle(
                        color = tint.copy(alpha = (if (dimmed) 0.25f else 0.7f) * appear),
                        radius = radius,
                        center = centre,
                        style = Stroke(width = 1.8f),
                    )
                } else {
                    drawCircle(
                        color = tint.copy(alpha = (if (dimmed) 0.18f else 0.35f + 0.45f * vitality) * appear),
                        radius = radius,
                        center = centre,
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = (if (dimmed) 0.06f else 0.18f + 0.5f * vitality) * appear),
                        radius = radius * 0.42f,
                        center = centre,
                    )
                }

                if (isSelected || isDragged) {
                    drawCircle(
                        color = Color.White.copy(alpha = if (isDragged) 1f else 0.9f),
                        radius = radius + 5f,
                        center = centre,
                        style = Stroke(width = 2f),
                    )
                }
            }

            // --- labels, laid out last so nothing draws over them --------------
            drawLabels(measurer, graph, positions, camera, selected, neighbours, appear) { radiusOf(it) }
            selected?.let { drawEdgeLabels(measurer, graph, positions, camera, it, appear) }
        }

        MapControls(
            scale = camera.scale,
            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
            onZoomIn = {
                camera = camera.zoomedAround(viewport.centre(), 1.6f, MIN_SCALE, MAX_SCALE)
            },
            onZoomOut = {
                camera = camera.zoomedAround(viewport.centre(), 1f / 1.6f, MIN_SCALE, MAX_SCALE)
            },
            onFit = { fitRequest++ },
        )

        Text(
            "2本指で拡大・縮小／長押しでノードを動かす",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.45f),
            modifier = Modifier.align(Alignment.TopStart).padding(12.dp).fillMaxWidth(0.62f),
        )

        selected?.let { index ->
            NodePanel(
                graph = graph,
                index = index,
                modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp),
                onOpen = { onOpenNote(graph.nodes[index].node) },
                onFocus = { onFocus(graph.nodes[index].node.noteId) },
                onDismiss = { selected = null },
            )
        }
    }
}

/**
 * The dates the map can be scrubbed to.
 *
 * The exam is inserted as its own stop when one is set, because "what will this look
 * like on the day" is the question the whole countdown exists to answer.
 */
internal fun horizonStops(examDays: Int?): List<Int> =
    buildList {
        addAll(listOf(0, 3, 7, 14, 30, 60, 90))
        if (examDays != null && examDays > 0) add(examDays)
    }.distinct().sorted()

/**
 * Scrub the map forward in time.
 *
 * Nothing is scheduled or changed — the forgetting curve is simply evaluated at a
 * later date, so the dim patches show what will have gone by then if left alone.
 */
@Composable
private fun TimeSlider(
    stops: List<Int>,
    index: Float,
    onIndexChange: (Float) -> Unit,
    horizonDays: Int,
    examDays: Int?,
    meanStrength: Double?,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                when {
                    horizonDays == 0 -> "いまの記憶"
                    examDays != null && horizonDays == examDays -> "試験日（${horizonDays}日後）の予測"
                    else -> "${horizonDays}日後の予測"
                },
                style = MaterialTheme.typography.labelLarge,
            )
            meanStrength?.let {
                Text(
                    "平均 ${(it * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Slider(
            value = index,
            onValueChange = onIndexChange,
            valueRange = 0f..(stops.size - 1).toFloat(),
            steps = (stops.size - 2).coerceAtLeast(0),
        )
        Text(
            "動かすと、そのままにした場合に何が消えていくかが見えます。",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun Size.centre(): Offset =
    if (this == Size.Zero) Offset(1f, 1f) else Offset(width / 2f, height / 2f)

@Composable
private fun MapControls(
    scale: Float,
    modifier: Modifier = Modifier,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onFit: () -> Unit,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White.copy(alpha = 0.10f),
        ) {
            Column {
                ControlButton("＋", onZoomIn)
                ControlButton("－", onZoomOut)
            }
        }
        Spacer(Modifier.height(8.dp))
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White.copy(alpha = 0.10f),
            onClick = onFit,
        ) {
            Text(
                "全体",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "${(scale * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.5f),
        )
    }
}

@Composable
private fun ControlButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.Bold)
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
    val dx = p1.x - p0.x
    val dy = p1.y - p0.y
    if (hypot(dx, dy) < 0.5f) return
    val control = controlPointFor(p0, p1)

    drawPath(
        Path().apply {
            moveTo(p0.x, p0.y)
            quadraticTo(control.x, control.y, p1.x, p1.y)
        },
        color,
        style = Stroke(width = width),
    )

    if (travel != null && signalStrength > 0f) {
        val centre = pointOnAxon(p0, control, p1, travel)
        // Fade the signal in and out so it does not pop at the ends.
        val fade = (1f - abs(0.5f - travel) * 2f).coerceIn(0f, 1f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.85f * fade * signalStrength), color.copy(alpha = 0f)),
                center = centre,
                radius = 9f,
            ),
            radius = 9f,
            center = centre,
        )
    }
}

private fun controlPointFor(p0: Offset, p1: Offset): Offset {
    val dx = p1.x - p0.x
    val dy = p1.y - p0.y
    // Bow the line perpendicular to itself so parallel links stay distinguishable.
    return Offset((p0.x + p1.x) / 2 - dy * CURVATURE, (p0.y + p1.y) / 2 + dx * CURVATURE)
}

private fun pointOnAxon(p0: Offset, control: Offset, p1: Offset, t: Float): Offset {
    val inv = 1 - t
    return Offset(
        inv * inv * p0.x + 2 * inv * t * control.x + t * t * p1.x,
        inv * inv * p0.y + 2 * inv * t * control.y + t * t * p1.y,
    )
}

/** Short faint spurs that read as dendrites. Angles are fixed per node. */
private fun DrawScope.drawDendrites(centre: Offset, radius: Float, color: Color, seed: Int) {
    for (i in 0 until 4) {
        val angle = ((seed * 47 + i * 90) % 360) * Math.PI / 180.0
        drawLine(
            color,
            Offset(centre.x + (radius * 0.95f * cos(angle)).toFloat(), centre.y + (radius * 0.95f * sin(angle)).toFloat()),
            Offset(centre.x + (radius * 1.9f * cos(angle)).toFloat(), centre.y + (radius * 1.9f * sin(angle)).toFloat()),
            strokeWidth = 1.2f,
        )
    }
}

/**
 * Draw as many node labels as will fit without overlapping.
 *
 * Labels are placed in order of how much they matter right now, and one that would
 * collide with an already-placed label is dropped — an unreadable pile of overlapping
 * text is worse than fewer names.
 */
private fun DrawScope.drawLabels(
    measurer: TextMeasurer,
    graph: LaidOutGraph,
    positions: List<Offset>,
    camera: GraphCamera,
    selected: Int?,
    neighbours: Set<Int>,
    appear: Float,
    radiusOf: (Int) -> Float,
) {
    val order = graph.nodes.indices.sortedByDescending { index ->
        when {
            index == selected -> 1_000_000
            index in neighbours -> 100_000 + graph.nodes[index].node.degree
            else -> graph.nodes[index].node.degree
        }
    }

    // Seed the occupied regions with the nodes themselves, so a label never lands on
    // top of a neighbouring cell body.
    val placed = graph.nodes.indices.mapNotNullTo(mutableListOf()) { index ->
        val centre = camera.toScreen(positions[index])
        val r = radiusOf(index)
        if (centre.x < -r || centre.y < -r || centre.x > size.width + r || centre.y > size.height + r) null
        else Rect(centre.x - r, centre.y - r, centre.x + r, centre.y + r)
    }
    var drawn = 0
    for (index in order) {
        if (drawn >= MAX_LABELS) break
        val node = graph.nodes[index].node
        val dimmed = selected != null && index != selected && index !in neighbours
        // At a distance only the hubs are named; zoom in and the rest appear.
        if (dimmed && camera.scale < 1.2f) continue
        if (selected == null && camera.scale < 0.8f && node.degree < 3) continue

        val centre = camera.toScreen(positions[index])
        if (centre.x < 0f || centre.y < 0f || centre.x > size.width || centre.y > size.height) continue

        val text = shortLabel(node.title)
        val layout: TextLayoutResult = measurer.measure(
            text,
            TextStyle(
                color = Color.White.copy(alpha = (if (dimmed) 0.3f else 0.92f) * appear),
                fontSize = 12.sp,
                fontWeight = if (index == selected) FontWeight.Bold else FontWeight.Medium,
            ),
        )
        // Nudge a label that would run off the edge back inside, rather than letting
        // it be clipped mid-word.
        val labelX = (centre.x - layout.size.width / 2f)
            .coerceIn(4f, (size.width - layout.size.width - 4f).coerceAtLeast(4f))
        val topLeft = Offset(labelX, centre.y + radiusOf(index) + 6f)
        if (topLeft.y + layout.size.height > size.height) continue
        val rect = Rect(
            topLeft.x - 3f,
            topLeft.y - 2f,
            topLeft.x + layout.size.width + 3f,
            topLeft.y + layout.size.height + 2f,
        )
        if (placed.any { it.overlaps(rect) }) continue

        placed += rect
        drawn++
        drawText(layout, topLeft = topLeft)
    }
}

/** Name the relations of the selected node, so the map says what a link means. */
private fun DrawScope.drawEdgeLabels(
    measurer: TextMeasurer,
    graph: LaidOutGraph,
    positions: List<Offset>,
    camera: GraphCamera,
    selected: Int,
    appear: Float,
) {
    val incident = graph.edges.filter { it.fromIndex == selected || it.toIndex == selected }.take(8)
    val placed = mutableListOf<Rect>()
    for (edge in incident) {
        val p0 = camera.toScreen(positions[edge.fromIndex])
        val p1 = camera.toScreen(positions[edge.toIndex])
        val mid = pointOnAxon(p0, controlPointFor(p0, p1), p1, 0.5f)
        val layout = measurer.measure(
            edge.label,
            TextStyle(color = Color.White.copy(alpha = 0.85f * appear), fontSize = 10.sp),
        )
        val topLeft = Offset(mid.x - layout.size.width / 2f, mid.y - layout.size.height / 2f)
        val rect = Rect(
            topLeft.x - 5f, topLeft.y - 3f,
            topLeft.x + layout.size.width + 5f, topLeft.y + layout.size.height + 3f,
        )
        if (placed.any { it.overlaps(rect) }) continue
        placed += rect
        drawRoundRect(
            color = FIELD_BACKGROUND.copy(alpha = 0.85f * appear),
            topLeft = Offset(rect.left, rect.top),
            size = Size(rect.width, rect.height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
        )
        drawText(layout, topLeft = topLeft)
    }
}

@Composable
private fun NodePanel(
    graph: LaidOutGraph,
    index: Int,
    modifier: Modifier = Modifier,
    onOpen: () -> Unit,
    onFocus: () -> Unit,
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
        modifier = modifier.fillMaxWidth().heightIn(max = 300.dp),
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
                Pill(if (node.isNew) "未学習" else "思い出しやすさ ${(node.strength * 100).toInt()}%")
            }
            if (connections.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Column(
                    Modifier.heightIn(max = 96.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    connections.forEach { (label, title) ->
                        Text(
                            "$label → $title",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onOpen, modifier = Modifier.weight(1f)) { Text("開く") }
                if (node.degree > 0) {
                    OutlinedButton(onClick = onFocus, modifier = Modifier.weight(1f)) { Text("周辺だけ") }
                }
                OutlinedButton(onClick = onDismiss, modifier = Modifier.width(80.dp)) { Text("閉じる") }
            }
        }
    }
}

@Composable
private fun Legend(graph: LaidOutGraph, modifier: Modifier = Modifier) {
    val present = remember(graph) { graph.nodes.map { it.node.typeId }.distinct() }
    Row(
        modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        present.forEach { typeId ->
            Pill(
                NoteType.fromId(typeId).label,
                colorFor(typeId).copy(alpha = 0.25f),
                MaterialTheme.colorScheme.onSurface,
            )
        }
        Pill("明るい = 覚えている", Color.Transparent, MaterialTheme.colorScheme.onSurfaceVariant)
        Pill("輪郭だけ = 未学習", Color.Transparent, MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Trim a title to a fixed display width rather than a character count.
 *
 * A Japanese glyph is about twice as wide as a Latin one, so counting characters
 * either overflows on Japanese or throws away most of a Latin label. Budgeting by
 * width keeps mixed titles — "0.010 mol/L の塩酸の pH" — informative instead of
 * cutting them off inside the number.
 */
internal fun shortLabel(title: String, budget: Int = 22): String {
    var used = 0
    val out = StringBuilder()
    for (character in title.trim()) {
        val width = if (character.isWide()) 2 else 1
        if (used + width > budget) {
            out.append('…')
            break
        }
        out.append(character)
        used += width
    }
    return out.toString()
}

private fun Char.isWide(): Boolean =
    code in 0x1100..0x115F || code in 0x2E80..0xA4CF || code in 0xAC00..0xD7A3 ||
        code in 0xF900..0xFAFF || code in 0xFE30..0xFE6F || code in 0xFF00..0xFF60 ||
        code in 0xFFE0..0xFFE6

private const val MAX_LABELS = 60

/** One hue per note type, chosen to stay legible on the dark field. */
private fun colorFor(typeId: String): Color = when (typeId) {
    NoteType.ENGLISH.id -> Color(0xFF5B9BFF)
    NoteType.CHEM_SUBSTANCE.id -> Color(0xFF2DD4BF)
    NoteType.CHEM_REACTION.id -> Color(0xFFFBBF24)
    NoteType.EISAKUBUN.id -> Color(0xFFC084FC)
    NoteType.CHEM_CALC.id -> Color(0xFF4ADE80)
    else -> Color(0xFF94A3B8)
}
