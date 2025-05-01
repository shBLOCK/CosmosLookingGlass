@file:Suppress("NOTHING_TO_INLINE")

package ui

import de.fabmax.kool.KoolContext
import de.fabmax.kool.math.Vec2d
import de.fabmax.kool.modules.ui2.*
import utils.RectF

abstract class FreeLayoutUiModifier(surface: UiSurface) : UiModifier(surface) {
    var freePos by property(Vec2d.ZERO)
    var freeAnchor by property(Vec2d.ZERO)
}

object FreeLayout : Layout {
    override fun measureContentSize(uiNode: UiNode, ctx: KoolContext) = uiNode.run {
        setContentSize(widthPx, heightPx)
    }

    override fun layoutChildren(uiNode: UiNode, ctx: KoolContext) {
        uiNode.children.forEach { child ->
            val rect = layout(uiNode, child)
            child.setBounds(rect.minX, rect.minY, rect.maxX, rect.maxY)
        }
    }

    fun layout(parent: UiNode, child: UiNode): RectF {
        val modifier = (child.modifier as? FreeLayoutUiModifier)
            ?: throw IllegalArgumentException("$child not using FreeLayoutModifier")
        val childWidth = child.computeWidthFromDimension(Dp.UNBOUNDED.px)
        val childHeight = child.computeHeightFromDimension(Dp.UNBOUNDED.px)
        val childX = parent.leftPx + modifier.freePos.x - childWidth * modifier.freeAnchor.x
        val childY = parent.topPx + modifier.freePos.y - childHeight * modifier.freeAnchor.y
        return RectF(childX.toFloat(), childY.toFloat(), childWidth, childHeight)
    }
}

inline fun FreeLayoutUiModifier.freePos(pos: Vec2d) = this.apply { freePos = pos }
inline fun FreeLayoutUiModifier.freeAnchor(anchor: Vec2d) = this.apply { freeAnchor = anchor }
inline fun FreeLayoutUiModifier.freeAnchor(anchor: AlignmentXY) = this.apply { freeAnchor = anchor.anchor }