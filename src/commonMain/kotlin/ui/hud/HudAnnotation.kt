@file:OptIn(ExperimentalContracts::class)
@file:Suppress("FunctionName")

package ui.hud

import de.fabmax.kool.KoolContext
import de.fabmax.kool.math.MutableVec3d
import de.fabmax.kool.math.Vec2d
import de.fabmax.kool.math.Vec3d
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.pipeline.RenderPass
import ui.*
import utils.RectF
import utils.rect
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class HudAnnotationModifier(surface: UiSurface) : FreeLayoutUiModifier(surface) {
    var poiPos: Vec2d? by property(null)

    /** Acceptable alignments, sorted by preference.
     *
     *
     * Will try these alignments one by one until the content fits in the viewport.
     * If it never fits, use the alignment that makes most of the annotation visible.
     * */
    var alignments: List<AlignmentXY> by property { listOf(AlignmentXY.Right) }
}

interface HudAnnotationScope : UiScope {
    override val modifier: HudAnnotationModifier
}

class HudAnnotation(parent: UiNode?, surface: UiSurface) : BoxNode(parent, surface), HudAnnotationScope {
    override val modifier = HudAnnotationModifier(surface)

    private fun getMarginOffsetForAlignment(alignment: AlignmentXY) = Vec2d(
        when (alignment.x) {
            AlignmentX.Start -> marginStartPx
            AlignmentX.Center -> 0F
            AlignmentX.End -> -marginEndPx
        }.toDouble(),
        when (alignment.y) {
            AlignmentY.Top -> marginTopPx
            AlignmentY.Center -> 0F
            AlignmentY.Bottom -> -marginBottomPx
        }.toDouble()
    )

    override fun measureContentSize(ctx: KoolContext) {
        super.measureContentSize(ctx)

        // Super hacky solution...
        // By the time layoutChildren() is called, our own layout has already been done by FreeLayout.
        // Thus, we insert a bit of logic here to find the best alignment and set out free layout parameters.
        val poi = checkNotNull(modifier.poiPos) { "No POI set for $this" }
        val parent = parent
        check(parent is HudViewport)
        val viewport = parent.rect
        var maxArea = 0F
        var maxAreaAlignment: AlignmentXY? = null
        for (alignment in modifier.alignments) {
            // Also, many "global states" involved here (the modifiers), definitely not pretty...
            modifier
                .freeAnchor(alignment)
                .freePos(poi + getMarginOffsetForAlignment(alignment))
            val rect = FreeLayout.layout(parent, this)
            if (rect in viewport) {
                maxAreaAlignment = null
                break
            }
            val area = RectF.overlap(rect, viewport).area
            if ((area - maxArea) > 1F) {
                maxArea = area
                maxAreaAlignment = alignment
            }
        }
        maxAreaAlignment?.let {
            modifier
                .freeAnchor(it)
                .freePos(poi + getMarginOffsetForAlignment(it))
        }
    }

    companion object {
        val DEFAULT = listOf(
            AlignmentXY.Right.mirror,
            AlignmentXY.Left.mirror,
            AlignmentXY.Top.mirror,
            AlignmentXY.Bottom.mirror,
            AlignmentXY.TopRight.mirror,
            AlignmentXY.TopLeft.mirror,
            AlignmentXY.BottomRight.mirror,
            AlignmentXY.BottomLeft.mirror
        )
        val CENTER = listOf(AlignmentXY.Center)
    }
}

fun UiScope.HudAnnotation(
    pos: Vec2d,
    alignments: List<AlignmentXY> = HudAnnotation.DEFAULT,
    block: HudAnnotationScope.() -> Unit
) {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    uiNode.createChild(null, HudAnnotation::class, ::HudAnnotation).apply annotation@{
        modifier.poiPos = pos
        modifier.alignments = alignments
        this@annotation.block()
    }
}

fun UiScope.HudAnnotation(
    worldPos: Vec3d,
    view: RenderPass.View,
    alignments: List<AlignmentXY> = HudAnnotation.DEFAULT,
    block: HudAnnotationScope.() -> Unit
) {
    val pos = MutableVec3d()
    view.camera.projectViewport(worldPos, view.viewport, pos)
    HudAnnotation(pos.xy, alignments, block)
}