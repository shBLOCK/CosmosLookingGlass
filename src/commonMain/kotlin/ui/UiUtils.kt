@file:Suppress("NOTHING_TO_INLINE")

package ui

import de.fabmax.kool.math.Vec2d
import de.fabmax.kool.modules.ui2.AlignmentX
import de.fabmax.kool.modules.ui2.AlignmentY
import de.fabmax.kool.modules.ui2.Dp
import de.fabmax.kool.modules.ui2.UiModifier
import de.fabmax.kool.modules.ui2.align
import de.fabmax.kool.modules.ui2.margin

inline val Float.dpx get() = Dp.fromPx(this)
inline val Double.dpx get() = Dp.fromPx(this.toFloat())

fun UiModifier.free(
    x: Double, y: Double,
    alignX: AlignmentX = AlignmentX.Start, alignY: AlignmentY = AlignmentY.Top
) {
    margin(start = x.dpx, top = y.dpx, end = Dp.ZERO, bottom = Dp.ZERO)
    align(alignX, alignY)
}

inline fun UiModifier.free(pos: Vec2d, alignX: AlignmentX = AlignmentX.Start, alignY: AlignmentY = AlignmentY.Top) =
    free(pos.x, pos.y, alignX, alignY)