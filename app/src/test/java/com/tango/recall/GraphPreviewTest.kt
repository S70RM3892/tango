package com.tango.recall

import androidx.test.core.app.ApplicationProvider
import com.tango.recall.data.Repository
import com.tango.recall.data.Seed
import com.tango.recall.data.TangoDb
import com.tango.recall.ui.screens.ForceLayout
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import kotlin.math.hypot

/**
 * Structural checks on the map the app actually ships with.
 *
 * A force layout can technically "succeed" and still be an unreadable ball, so these
 * assert the properties that make it legible. Setting TANGO_GRAPH_DUMP also writes the
 * laid-out coordinates to that path, for rendering a preview outside the JVM.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class GraphPreviewTest {

    private lateinit var repo: Repository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        context.deleteDatabase(TangoDb.DB_NAME)
        repo = Repository(TangoDb(context))
        Seed.populate(repo)
    }

    @Test
    fun theShippedMapIsSpreadOutAndConnected() {
        val data = repo.graph(null)
        assertTrue("seed should give a map worth drawing", data.nodes.size >= 40)
        assertTrue(data.edges.size >= 30)

        // Lay out in a phone's shape, the way the screen actually asks for it.
        val laid = ForceLayout.layout(data, width = 720f, height = 1000f)
        dumpIfRequested(laid)

        // No two notes may sit on top of each other, or the map is unreadable.
        var closest = Float.MAX_VALUE
        for (i in laid.nodes.indices) {
            for (j in i + 1 until laid.nodes.size) {
                closest = minOf(
                    closest,
                    hypot(laid.nodes[i].x - laid.nodes[j].x, laid.nodes[i].y - laid.nodes[j].y),
                )
            }
        }
        assertTrue("notes overlapped (closest pair ${closest}px of ${laid.width})", closest > laid.width * 0.02f)

        // Linked notes must end up meaningfully closer than the map's average pair.
        val linked = laid.edges.map {
            hypot(
                laid.nodes[it.fromIndex].x - laid.nodes[it.toIndex].x,
                laid.nodes[it.fromIndex].y - laid.nodes[it.toIndex].y,
            )
        }.average()
        var total = 0.0
        var count = 0
        for (i in laid.nodes.indices) {
            for (j in i + 1 until laid.nodes.size) {
                total += hypot(laid.nodes[i].x - laid.nodes[j].x, laid.nodes[i].y - laid.nodes[j].y)
                count++
            }
        }
        val average = total / count
        assertTrue("related notes are not clustering (linked=$linked average=$average)", linked < average * 0.6)

        // And the whole thing should actually use the canvas, not huddle in the middle.
        val spanX = laid.nodes.maxOf { it.x } - laid.nodes.minOf { it.x }
        val spanY = laid.nodes.maxOf { it.y } - laid.nodes.minOf { it.y }
        assertTrue(
            "layout does not fill the screen (spanX=$spanX spanY=$spanY)",
            spanX > laid.width * 0.7f && spanY > laid.height * 0.7f,
        )
    }


    @Test
    fun notesOfOneGroupSettleTogether() {
        val laid = ForceLayout.layout(repo.graph(null), width = 720f, height = 1000f)
        // 語根や分野ごとのまとまりが、地図の上でも島になっているか。小さな群れは放って
        // おいても固まるので、崩れやすい大きな群れ（6語以上）だけを見る。ここが緩むと、
        // 囲いを描いても「散らばった点を囲んだ枠」にしかならない。
        val groups = laid.nodes.indices
            .groupBy { laid.nodes[it].node.cluster }
            .filterKeys { it.isNotBlank() }
            .filterValues { it.size >= 6 }
        assertTrue("seed should have big groups to gather", groups.size >= 15)

        val diagonal = hypot(laid.width, laid.height)
        val loose = groups.filter { (_, members) ->
            val cx = members.map { laid.nodes[it].x }.average().toFloat()
            val cy = members.map { laid.nodes[it].y }.average().toFloat()
            members.map { hypot(laid.nodes[it].x - cx, laid.nodes[it].y - cy) }
                .average() > diagonal * 0.12
        }.keys
        assertTrue("groups smeared across the map: $loose", loose.size <= 1)
    }

    private fun dumpIfRequested(laid: com.tango.recall.ui.screens.LaidOutGraph) {
        val path = System.getenv("TANGO_GRAPH_DUMP") ?: return
        val json = buildString {
            append("{\"width\":${laid.width},\"height\":${laid.height},\"nodes\":[")
            laid.nodes.forEachIndexed { i, positioned ->
                if (i > 0) append(",")
                val node = positioned.node
                append(
                    "{\"x\":${positioned.x},\"y\":${positioned.y},\"type\":\"${node.typeId}\"," +
                        "\"degree\":${node.degree},\"strength\":${node.strength}," +
                        "\"isNew\":${node.isNew},\"cluster\":\"${node.cluster.replace("\"", "'").replace("\\", "")}\"," +
                        "\"title\":\"${node.title.replace("\"", "'").replace("\\", "")}\"}"
                )
            }
            append("],\"edges\":[")
            laid.edges.forEachIndexed { i, edge ->
                if (i > 0) append(",")
                append("{\"a\":${edge.fromIndex},\"b\":${edge.toIndex},\"type\":\"${edge.typeId}\"}")
            }
            append("]}")
        }
        File(path).writeText(json)
    }
}
