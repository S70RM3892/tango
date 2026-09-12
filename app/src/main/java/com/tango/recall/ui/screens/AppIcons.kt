package com.tango.recall.ui.screens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * The glyphs this app needs that aren't in material-icons-core.
 *
 * Defined here rather than pulling in material-icons-extended, which would add
 * several thousand unused vectors to the APK.
 */
object AppIcons {

    private fun icon(name: String, fillType: PathFillType, pathData: PathBuilderScope.() -> Unit) =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black), pathFillType = fillType) {
                PathBuilderScope(this).pathData()
            }
        }.build()

    /** Three ascending bars. */
    val BarChart: ImageVector by lazy {
        icon("BarChart", PathFillType.NonZero) {
            rect(5f, 9.2f, 3f, 9.8f)
            rect(10.6f, 5f, 2.8f, 14f)
            rect(16.2f, 13f, 2.8f, 6f)
        }
    }

    /** A filled circle with two pause bars punched out of it. */
    val PauseCircle: ImageVector by lazy {
        icon("PauseCircle", PathFillType.EvenOdd) {
            with(builder) {
                moveTo(12f, 2f)
                curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
                reflectiveCurveToRelative(4.48f, 10f, 10f, 10f)
                reflectiveCurveToRelative(10f, -4.48f, 10f, -10f)
                reflectiveCurveTo(17.52f, 2f, 12f, 2f)
                close()
            }
            rect(9f, 7f, 2f, 10f)
            rect(13f, 7f, 2f, 10f)
        }
    }

    /** A curved arrow turning back on itself. */
    val Undo: ImageVector by lazy {
        icon("Undo", PathFillType.NonZero) {
            with(builder) {
                moveTo(12.5f, 8f)
                curveToRelative(-2.65f, 0f, -5.05f, 0.99f, -6.9f, 2.6f)
                lineTo(2f, 7f)
                verticalLineToRelative(9f)
                horizontalLineToRelative(9f)
                lineToRelative(-3.62f, -3.62f)
                curveToRelative(1.39f, -1.16f, 3.16f, -1.88f, 5.12f, -1.88f)
                curveToRelative(3.54f, 0f, 6.55f, 2.31f, 7.6f, 5.5f)
                lineToRelative(2.37f, -0.78f)
                curveTo(21.08f, 11.03f, 17.15f, 8f, 12.5f, 8f)
                close()
            }
        }
    }

    /** A play triangle against a bar. */
    val SkipNext: ImageVector by lazy {
        icon("SkipNext", PathFillType.NonZero) {
            with(builder) {
                moveTo(6f, 18f)
                lineToRelative(8.5f, -6f)
                lineTo(6f, 6f)
                close()
            }
            rect(16f, 6f, 2f, 12f)
        }
    }
}

/** Thin wrapper so the icon definitions above can share a `rect` helper. */
class PathBuilderScope(val builder: androidx.compose.ui.graphics.vector.PathBuilder) {
    fun rect(x: Float, y: Float, w: Float, h: Float) {
        with(builder) {
            moveTo(x, y)
            horizontalLineToRelative(w)
            verticalLineToRelative(h)
            horizontalLineToRelative(-w)
            close()
        }
    }
}
