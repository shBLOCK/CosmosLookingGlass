@file:Suppress("NOTHING_TO_INLINE")

package ui.hud

import de.fabmax.kool.KoolContext
import de.fabmax.kool.math.MutableVec2f
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.util.Color
import de.fabmax.kool.util.MutableColor
import ui.*
import utils.*

class HudOutlineSphereButtonModifier(surface: UiSurface) : FreeLayoutUiModifier(surface) {
    var color by property(Color.WHITE)
    var radius by property { Dp(12F) }
    var priority by property(0)
    var onButtonClick: (() -> Unit)? by property(null)
    var onButtonDoubleClick: (() -> Unit)? by property(null)
}

interface HudOutlineSphereButtonScope : UiScope {
    override val modifier: HudOutlineSphereButtonModifier
    val hovered: Boolean
    val pressed: Boolean
    var hidden: Boolean
}

private val HOVER_EXPANSION = Dp(3F)

class HudOutlineSphereButtonNode(override val stateStore: KeyedStateStore, parent: UiNode?, surface: UiSurface) :
    UiNode(parent, surface), HudOutlineSphereButtonScope, KeyedStateStore.Managed {

    override val modifier = HudOutlineSphereButtonModifier(surface)
    private val weakProps = WeakProps()

    override var hovered by weakProps { keyedState { false } }.unwrap()
    private val hoverAnimator by weakProps { keyed { AnimatedFloatBidir(0.2F) } }
    override var pressed by weakProps { keyedState { false } }.unwrap()
    private val pressAnimator by weakProps { keyed { AnimatedFloatBidir(0.1F) } }
    override var hidden by weakProps { keyedState { false } }.unwrap()
    private val hideAnimator by weakProps { keyed { AnimatedFloatBidir(0.2F) } }

    lateinit var projection: ViewportSphereCameraProjectionResult

    fun setup(projection: ViewportSphereCameraProjectionResult) {
        weakProps.initialize()

        modifier.onClick += ::onPointerEvent
        modifier.onEnter += ::onPointerEvent
        modifier.onHover += ::onPointerEvent
        modifier.onExit += ::onPointerEvent
        modifier.onDrag += ::onPointerEvent

        this.projection = projection
        hidden = false

        val size = modifier.radius * 2 + HOVER_EXPANSION * 2 + 2.dp
        modifier.size(size, size)

        modifier
            .freePos(projection.center)
            .freeAnchor(AlignmentXY.Center)

        if (projection.centerDistance <= 0.0) hidden = true

        if (projection.majorRadius > modifier.radius.px)
            hidden = true
    }

    override fun render(ctx: KoolContext) {
        hoverAnimator.start(hovered.toFloat())
        if (pressed) {
            pressAnimator.start(1F)
        } else {
            if (pressAnimator.value == 1F) pressAnimator.start(0F)
        }
        hideAnimator.start(hidden.toFloat())

        hoverAnimator.progressAndUse()
        pressAnimator.progressAndUse()
        hideAnimator.progressAndUse()

        getUiPrimitives().apply {
            var radius = modifier.radius
            var thickness = 2.dp
            val color = MutableColor(modifier.color)

            radius += HOVER_EXPANSION * hoverAnimator.value
            radius *= mix(1F, 0.8F, pressAnimator.value)
            thickness *= mix(1F, 2F, hoverAnimator.value)
            color.a = mix(0.75F, 1F, hoverAnimator.value)
            color.a *= 1F - hideAnimator.value

            localCircleBorder(
                widthPx / 2,
                heightPx / 2,
                radius.px,
                thickness.px,
                color
            )
        }

        super.render(ctx)
    }

    private fun onPointerEvent(event: PointerEvent) {
        if (hidden) {
            hovered = false
            pressed = false
            event.reject()
            return
        }

        val ptr = event.pointer
        if (ptr.isConsumed()) return
        if (pressed) ptr.consume()
        if (ptr.isLeftButtonReleased) pressed = false
        if (event.position.distance(MutableVec2f(widthPx, heightPx) / 2F) < modifier.radius.px) {
            ptr.consume()
            hovered = true
            if (ptr.isLeftButtonPressed) pressed = true
            if (event.isLeftDoubleClick) {
                modifier.onButtonDoubleClick?.invoke()
            } else if (event.isLeftClick) {
                modifier.onButtonClick?.invoke()
            }
        } else hovered = false

        if (!ptr.isConsumed()) event.reject()
    }
}

inline fun HudOutlineSphereButtonScope.overlaps(other: HudOutlineSphereButtonScope) =
    this.modifier.freePos.sqrDistance(other.modifier.freePos) < sqr(this.modifier.radius.px + other.modifier.radius.px)