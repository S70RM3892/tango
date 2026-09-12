package com.tango.recall.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tango.recall.data.Department
import com.tango.recall.data.Universities
import com.tango.recall.data.UniversityFilter
import com.tango.recall.data.UniversityGroup
import com.tango.recall.data.UniversitySort
import com.tango.recall.ui.AppViewModel
import kotlin.math.pow
import kotlin.math.roundToInt

private const val MAP_MIN_SCALE = 0.6f
private const val MAP_MAX_SCALE = 12f
private val MAP_BACKGROUND = Color(0xFF0A0E1A)

/**
 * The national and public university database.
 *
 * Everything filters everything: tapping a prefecture on the map, a field in the
 * composition bar, or a chip all narrow the same set, and the map re-shades to show
 * only what is left. Source: 文部科学省「令和7年度 全国大学一覧」.
 */
@Composable
fun UniversityScreen(vm: AppViewModel, nav: NavController) {
    LaunchedEffect(Unit) { vm.loadUniversities() }

    var filter by remember { mutableStateOf(UniversityFilter()) }
    var sort by remember { mutableStateOf(UniversitySort.CAPACITY_DESC) }
    var shortlistOnly by remember { mutableStateOf(false) }
    var showMap by remember { mutableStateOf(true) }
    var expanded by remember { mutableStateOf<String?>(null) }
    var sortMenu by remember { mutableStateOf(false) }

    val filtered = remember(vm.departments, filter, shortlistOnly, vm.shortlist) {
        Universities.filter(vm.departments, filter)
            .let { list -> if (shortlistOnly) list.filter { it.key in vm.shortlist } else list }
    }
    val groups = remember(filtered, sort) { Universities.groupByUniversity(filtered, sort) }
    val byPrefecture = remember(filtered) { Universities.capacityByPrefecture(filtered) }
    val composition = remember(filtered) { Universities.capacityByField(filtered) }

    Scaffold(
        topBar = {
            TangoTopBar("国公立大学", onBack = { nav.popBackStack() }) {
                TextButton(onClick = { showMap = !showMap }) {
                    Text(if (showMap) "地図を隠す" else "地図")
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            if (showMap) {
                JapanChoropleth(
                    map = vm.japanMap,
                    values = byPrefecture,
                    selected = filter.prefectures,
                    onToggle = { name ->
                        filter = filter.copy(
                            prefectures = if (name in filter.prefectures) filter.prefectures - name
                            else filter.prefectures + name,
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                )
            }

            OutlinedTextField(
                value = filter.query,
                onValueChange = { filter = filter.copy(query = it) },
                label = { Text("大学名・学部・学科で探す") },
                singleLine = true,
                trailingIcon = {
                    if (filter.query.isNotEmpty()) {
                        IconButton(onClick = { filter = filter.copy(query = "") }) {
                            Icon(Icons.Default.Clear, contentDescription = "消す")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            )

            ChipRow(
                options = Universities.KINDS,
                selected = filter.kinds,
                onToggle = { k ->
                    filter = filter.copy(kinds = if (k in filter.kinds) filter.kinds - k else filter.kinds + k)
                },
                leading = {
                    FilterChip(
                        selected = shortlistOnly,
                        onClick = { shortlistOnly = !shortlistOnly },
                        label = { Text("志望校 ${vm.shortlist.size}") },
                    )
                },
            )
            ChipRow(
                options = Universities.REGIONS,
                selected = filter.regions,
                onToggle = { r ->
                    filter = filter.copy(regions = if (r in filter.regions) filter.regions - r else filter.regions + r)
                },
            )
            ChipRow(
                options = Universities.FIELDS,
                selected = filter.fields,
                onToggle = { f ->
                    filter = filter.copy(fields = if (f in filter.fields) filter.fields - f else filter.fields + f)
                },
            )

            if (composition.isNotEmpty()) {
                CompositionBar(
                    composition = composition,
                    selected = filter.fields,
                    onToggle = { f ->
                        filter = filter.copy(fields = if (f in filter.fields) filter.fields - f else filter.fields + f)
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${groups.size} 大学 ・ ${filtered.size} 学科 ・ 定員 ${filtered.sumOf { it.capacity }} 人",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Box {
                    TextButton(onClick = { sortMenu = true }) { Text(sort.label) }
                    DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                        UniversitySort.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = { sort = option; sortMenu = false },
                            )
                        }
                    }
                }
                if (!filter.isEmpty || shortlistOnly) {
                    TextButton(onClick = { filter = UniversityFilter(); shortlistOnly = false }) {
                        Text("解除")
                    }
                }
            }

            when {
                vm.universitiesLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator()
                }

                groups.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text(
                        "条件に合う学科がありません。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                else -> {
                    val peak = groups.maxOf { it.capacity }.coerceAtLeast(1)
                    LazyColumn(
                        contentPadding = PaddingValues(12.dp, 4.dp, 12.dp, 32.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(groups, key = { it.university }) { group ->
                            UniversityRow(
                                group = group,
                                peak = peak,
                                expanded = expanded == group.university,
                                shortlist = vm.shortlist,
                                onExpand = {
                                    expanded = if (expanded == group.university) null else group.university
                                },
                                onShortlist = { vm.toggleShortlist(it) },
                            )
                        }
                        item {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "出典: 文部科学省「令和7年度 全国大学一覧」／" + JapanMapLoader.ATTRIBUTION,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChipRow(
    options: List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    leading: @Composable (() -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        leading?.invoke()
        options.forEach { option ->
            FilterChip(
                selected = option in selected,
                onClick = { onToggle(option) },
                label = { Text(option) },
            )
        }
    }
}

/** A single stacked bar showing what the current selection is made of, by field. */
@Composable
private fun CompositionBar(
    composition: List<Pair<String, Int>>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val total = composition.sumOf { it.second }.coerceAtLeast(1)
    Column(modifier) {
        Row(
            Modifier.fillMaxWidth().height(18.dp).clip(RoundedCornerShape(4.dp)),
        ) {
            composition.forEach { (field, capacity) ->
                Box(
                    Modifier
                        .weight(capacity.toFloat() / total)
                        .fillMaxSize()
                        .background(
                            fieldColour(field).copy(
                                alpha = if (selected.isEmpty() || field in selected) 1f else 0.25f
                            )
                        )
                        .clickable { onToggle(field) },
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            composition.take(6).forEach { (field, capacity) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(8.dp).height(8.dp).clip(RoundedCornerShape(2.dp)).background(fieldColour(field)))
                    Text(
                        " $field ${(capacity * 100.0 / total).roundToInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun UniversityRow(
    group: UniversityGroup,
    peak: Int,
    expanded: Boolean,
    shortlist: Set<String>,
    onExpand: () -> Unit,
    onShortlist: (Department) -> Unit,
) {
    SectionCard(modifier = Modifier.clickable(onClick = onExpand)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(group.university, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${group.kind} ・ ${group.prefecture} ・ ${group.departments.size} 学科",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text("${group.capacity} 人", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(6.dp))
        // The bar is the sort made visible: length is this university's share of the
        // largest one in the current selection.
        Box(
            Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(group.capacity.toFloat() / peak)
                    .height(6.dp)
                    .background(fieldColour(group.departments.first().field)),
            )
        }

        if (expanded) {
            Spacer(Modifier.height(10.dp))
            group.departments.forEach { department ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(department.label, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${department.field} ・ ${department.city} ・ ${department.years} 年制",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        "${department.capacity}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { onShortlist(department) }) {
                        Text(if (department.key in shortlist) "★" else "☆")
                    }
                }
            }
        }
    }
}

/**
 * Japan, shaded by how many places each prefecture holds in the current selection.
 *
 * Tapping a prefecture adds it to the filter, which re-shades the map — so the map is
 * both the picture and the control.
 */
@Composable
private fun JapanChoropleth(
    map: JapanMap,
    values: Map<String, Int>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val measurer = rememberTextMeasurer()
    var camera by remember(map) { mutableStateOf(GraphCamera.Identity) }
    var viewport by remember { mutableStateOf(Size.Zero) }

    LaunchedEffect(map, viewport) {
        if (viewport != Size.Zero && map.size != Size.Zero) {
            camera = GraphCamera.fit(map.size, viewport, padding = 8f)
        }
    }

    val peak = (values.values.maxOrNull() ?: 0).coerceAtLeast(1)
    val base = MaterialTheme.colorScheme.surfaceVariant
    val accent = MaterialTheme.colorScheme.primary
    val highlight = MaterialTheme.colorScheme.tertiary

    Canvas(
        modifier
            .background(MAP_BACKGROUND)
            .onSizeChanged { viewport = Size(it.width.toFloat(), it.height.toFloat()) }
            .pointerInput(map) {
                detectTapGestures { tap ->
                    map.prefectureAt(camera.toGraph(tap))?.let(onToggle)
                }
            }
            .pointerInput(map) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    camera = camera.panned(pan).zoomedAround(centroid, zoom, MAP_MIN_SCALE, MAP_MAX_SCALE)
                }
            },
    ) {
        if (map.prefectures.isEmpty()) return@Canvas

        map.inset?.let { box ->
            val topLeft = camera.toScreen(Offset(box.left, box.top))
            val bottomRight = camera.toScreen(Offset(box.right, box.bottom))
            drawRect(
                color = Color.White.copy(alpha = 0.25f),
                topLeft = topLeft,
                size = Size(bottomRight.x - topLeft.x, bottomRight.y - topLeft.y),
                style = Stroke(width = 1f),
            )
        }

        for (shape in map.prefectures) {
            val capacity = values[shape.name] ?: 0
            // Square-rooted so the middle of the range stays distinguishable rather
            // than everything below Tokyo looking identical.
            val intensity = (capacity.toFloat() / peak).pow(0.55f)
            val isSelected = shape.name in selected
            val fill = when {
                isSelected -> highlight
                capacity == 0 -> base.copy(alpha = 0.18f)
                else -> lerpColour(base.copy(alpha = 0.35f), accent, intensity)
            }
            for (ring in shape.polygons) {
                val path = Path().apply {
                    val first = camera.toScreen(ring.first())
                    moveTo(first.x, first.y)
                    for (i in 1 until ring.size) {
                        val p = camera.toScreen(ring[i])
                        lineTo(p.x, p.y)
                    }
                    close()
                }
                drawPath(path, fill)
                drawPath(
                    path,
                    Color.White.copy(alpha = if (isSelected) 0.9f else 0.28f),
                    style = Stroke(width = if (isSelected) 2f else 0.8f),
                )
            }

            // Names only once there is room for them.
            if (camera.scale > size.width / 900f || isSelected) {
                val anchor = camera.toScreen(shape.labelAnchor)
                if (anchor.x in 0f..size.width && anchor.y in 0f..size.height) {
                    val layout = measurer.measure(
                        shape.name.removeSuffix("県").removeSuffix("府").removeSuffix("都"),
                        TextStyle(
                            color = Color.White.copy(alpha = if (isSelected) 1f else 0.75f),
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        ),
                    )
                    drawText(
                        layout,
                        topLeft = Offset(
                            anchor.x - layout.size.width / 2f,
                            anchor.y - layout.size.height / 2f,
                        ),
                    )
                }
            }
        }
    }
}

private fun lerpColour(from: Color, to: Color, t: Float): Color {
    val f = t.coerceIn(0f, 1f)
    return Color(
        red = from.red + (to.red - from.red) * f,
        green = from.green + (to.green - from.green) * f,
        blue = from.blue + (to.blue - from.blue) * f,
        alpha = from.alpha + (to.alpha - from.alpha) * f,
    )
}

/** A stable hue per field, so a colour means the same thing on the bar and the list. */
internal fun fieldColour(field: String): Color {
    val index = Universities.FIELDS.indexOf(field).takeIf { it >= 0 } ?: Universities.FIELDS.lastIndex
    val hue = (index * 360f / Universities.FIELDS.size + 15f) % 360f
    return Color.hsl(hue, 0.55f, 0.58f)
}
