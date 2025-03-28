package ui

import de.fabmax.kool.KoolContext
import de.fabmax.kool.math.Vec2d
import de.fabmax.kool.modules.ui2.AlignmentX
import de.fabmax.kool.modules.ui2.AlignmentY
import de.fabmax.kool.modules.ui2.Dp
import de.fabmax.kool.modules.ui2.Layout
import de.fabmax.kool.modules.ui2.UiModifier
import de.fabmax.kool.modules.ui2.UiNode
import de.fabmax.kool.modules.ui2.align
import de.fabmax.kool.modules.ui2.margin

object FreeLayout : Layout {
    override fun measureContentSize(uiNode: UiNode, ctx: KoolContext) = uiNode.run {
        setContentSize(widthPx, heightPx)
    }

    override fun layoutChildren(uiNode: UiNode, ctx: KoolContext) {
        uiNode.also { parent ->
            parent.children.forEach { child ->
                val childWidth = child.computeWidthFromDimension(Dp.UNBOUNDED.px)
                val childHeight = child.computeHeightFromDimension(Dp.UNBOUNDED.px)
                val childX = parent.leftPx + when (child.modifier.alignX) {
                    AlignmentX.Start -> child.marginStartPx
                    AlignmentX.Center -> child.marginStartPx - childWidth / 2F
                    AlignmentX.End -> child.marginStartPx - childWidth
                }
                val childY = parent.topPx + when (child.modifier.alignY) {
                    AlignmentY.Top -> child.marginTopPx
                    AlignmentY.Center -> child.marginTopPx - childHeight / 2F
                    AlignmentY.Bottom -> child.marginTopPx - childHeight
                }
                child.setBounds(childX, childY, childX + childWidth, childY + childHeight)
            }
        }
    }
}

fun UiModifier.free(
    x: Double, y: Double,
    alignX: AlignmentX = AlignmentX.Start, alignY: AlignmentY = AlignmentY.Top
) {
    margin(start = x.dpx, top = y.dpx, end = Dp.ZERO, bottom = Dp.ZERO)
    align(alignX, alignY)
}

fun UiModifier.free(pos: Vec2d, alignX: AlignmentX = AlignmentX.Start, alignY: AlignmentY = AlignmentY.Top) =
    free(pos.x, pos.y, alignX, alignY)