package ui

import de.fabmax.kool.KoolContext
import de.fabmax.kool.math.*
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.util.Color
import de.fabmax.kool.util.Time
import utils.*
import kotlin.math.*

open class SlottedReboundSliderNode(parent: UiNode?, surface: UiSurface) :
    UiNode(parent, surface), Draggable, Hoverable {
    override val modifier = UiModifier(surface)

    fun setup() {
        modifier
            .dragListener(this)
            .hoverListener(this)
            .padding(vertical = handleSize + 3.dp, horizontal = handleSize + 10.dp)
    }

    val value = mutableStateOf(0.0)
    val pos = MappedStateValue(value, FwdInvFunction.identity())
    var posToValue: FwdInvFunction<Double, Double> by pos::function

    /**
     * Must be sorted.
     */
    var slots: List<Double> by mutableStateOf(listOf(-3.0, -2.0, -1.0, 0.0, 1.0, 2.0, 3.0))
    private val slotPosSequence get() = slots.asSequence().map(posToValue.inverse)
    private var slotAppearAnimator by mutableStateOf(0.0)
    var slotAppearRange = 5.0

    val slotInsertion = mutableStateOf(0.0)

    var lineWidth = 7.dp
    var handleSize = 10.dp
    var lineColor = Color.LIGHT_GRAY.withAlpha(0.5F)
    var handleColor = Color.WHITE
    val handleMiddleColor = Color.GRAY

    /**
     * The handle can (start to) go into a slot when
     * the distance between the handle and the slot are below this value.
     */
    var slotThreshold = 0.03

    /**
     * Help the user put the handle into the closest slot
     * when the distance between the drag position and the closest slot is below this value.
     */
    var slotAssistRange = 0.2

    private val valueRange get() = slots.first()..slots.last()
    private val posRange get() = posToValue.inverse(slots.first())..posToValue.inverse(slots.last())

    private val hoverAnimator = AnimatedFloatBidir(0.1F, 0.1F)
    private var dragPx by mutableStateOf(Vec2d.NaN)
    private inline val dragging get() = !dragPx.x.isNaN()
    private var hovering = false
    private val vel = MutableVec2d()

    protected fun posToPx(pos: Double) = paddingStartPx + ((pos - posRange.start) / posRange.width) * innerWidthPx
    protected fun posFromPx(px: Double) = posRange.start + ((px - paddingStartPx) / innerWidthPx) * posRange.width
    protected fun slotInsertionToPx(slotInsertion: Double) = (1.0 - slotInsertion) * innerHeightPx + paddingTopPx
    protected fun slotInsertionFromPx(px: Double) = (1.0 - (px - paddingTopPx) / innerHeightPx)
    protected fun handlePx() = Vec2d(posToPx(pos.value), slotInsertionToPx(slotInsertion.value))

    private fun update() {
        var pos = pos.use()
        var slotInsertion = slotInsertion.use()

        val dragTarget = if (dragging) Vec2d(
            posFromPx(dragPx.x),
            slotInsertionFromPx(dragPx.y)
        ) else Vec2d.NaN

        var remaining = Time.deltaT.toDouble()
        while (!remaining.isFuzzyZero()) {
            val dt = min(remaining, 1.0 / 50.0)
            remaining -= dt

            val target = MutableVec2d()

            val closestSlot = slotPosSequence.minBy { abs(pos - it) }
            val distToSlot = abs(pos - closestSlot)

            // y (slot insertion)
            if (dragging) {
                target.y =
                    if (distToSlot < slotThreshold) dragTarget.y else -1.0
            } else {
                target.y = if (slotInsertion < 0.5) -1.0 else 2.0
            }

            // x (pos on rail)
            val dragClosestSlot = slotPosSequence.minBy { abs(dragTarget.x - it) }
            if (slotInsertion > 0.0) {
                target.x = closestSlot
                pos = closestSlot
            } else {
                if (dragging) {
                    if (abs(dragTarget.x - dragClosestSlot) < slotAssistRange) {
                        target.x = mix(dragTarget.x, dragClosestSlot, (dragTarget.y / 2.0).clamp().pow(2.0))
                    } else {
                        target.x = dragTarget.x
                    }
                } else {
                    target.x = 0.0
                }
            }

            // spring physics
            val err = Vec2d(pos, slotInsertion) - target
            vel.x += (-err.x * 500.0 - vel.x * 25.0) * dt
            vel.y += (-err.y * 300.0 - vel.y * 25.0) * dt

            pos += vel.x * dt
            if (pos.isFuzzyZero() && vel.x.isFuzzyZero()) {
                pos = 0.0
                vel.x = 0.0
            }
            slotInsertion += vel.y * dt
            if (slotInsertion < 0.0 || slotInsertion > 1.0) {
                vel.y = 0.0
                slotInsertion = slotInsertion.clamp()
            }
        }

        this.pos.set(pos)
        this.slotInsertion.set(slotInsertion)

        slotAppearAnimator = slotAppearAnimator.expDecaySnapping(
            if (hovering || dragging || slotInsertion > 0.0) 1.0 else 0.0,
            16.0
        )
    }

    override fun render(ctx: KoolContext) {
        update()

        super.render(ctx)

        val handle = handlePx().toVec2f()
        val lineWidth = lineWidth.px
        val radius = lineWidth / 2F
        val railStartPx = posToPx(posRange.start).toFloat()
        val railEndPx = posToPx(posRange.endInclusive).toFloat()
        val railY = slotInsertionToPx(0.0).toFloat()
        val slotY = slotInsertionToPx(1.0).toFloat()

        with(getUiPrimitives(0)) {
            // rail
            localRect(railStartPx - radius, railY - radius, railEndPx - railStartPx + lineWidth, lineWidth, lineColor)
            localRectGradient(
                0F, railY - radius, railStartPx - radius, lineWidth,
                lineColor.withAlpha(0F), lineColor, 0F, 0F, railStartPx - radius, 0F
            )
            localRectGradient(
                railEndPx + radius, railY - radius, widthPx - railEndPx - radius, lineWidth,
                lineColor, lineColor.withAlpha(0F), railEndPx + radius, 0F, widthPx - railEndPx - radius, 0F
            )

            // slots
            for (slot in slots) {
                val slotPos = posToValue.inverse(slot)
                val slotPx = posToPx(slotPos).toFloat()
                val animatedSlotY = mix(
                    railY.toDouble(), slotY.toDouble(),
                    slotAppearAnimator * cos(((pos.value - slotPos) * PI / slotAppearRange).clamp(-PI, PI))
                ).toFloat()

                roundRect(
                    leftPx + slotPx - radius,
                    topPx + animatedSlotY - radius,
                    lineWidth,
                    railY - animatedSlotY + lineWidth,
                    radius,
                    Vec4f(clipLeftPx, clipTopPx, clipRightPx, topPx + railY - radius),
                    lineColor
                )
            }
        }

        with(getUiPrimitives(1)) {
            // handle
            localCircle(handle.x, handle.y, handleSize.px, handleColor)
            val hoverIndicatorLineWidth = 3F
            localCircleBorder(
                handle.x, handle.y,
                handleSize.px * (1F + hoverAnimator.progressAndUse() * (0.5F + 0.03F * sin(Time.gameTime * PI * 1.0).toFloat())) - hoverIndicatorLineWidth / 2F,
                hoverIndicatorLineWidth,
                handleColor.withAlpha(0.5F)
            )
            localCircle(handle.x, handle.y, lineWidth / 2F, handleMiddleColor)
        }
    }

    override fun onDragStart(ev: PointerEvent) {
        if (ev.position.toVec2d().distance(handlePx()) <= handleSize.px) {
            dragPx = ev.position.toVec2d()
        } else {
            ev.reject()
        }
    }

    override fun onDrag(ev: PointerEvent) {
        if (dragging) {
            dragPx = ev.position.toVec2d()
        } else {
            ev.reject()
        }
    }

    override fun onDragEnd(ev: PointerEvent) {
        if (dragging) {
            dragPx = Vec2d.NaN
        } else {
            ev.reject()
        }
    }

    override fun onHover(ev: PointerEvent) {
        if (ev.position.toVec2d().distance(handlePx()) <= handleSize.px) {
            hovering = true
            hoverAnimator.start(1F)
        } else {
            hovering = false
            hoverAnimator.start(0F)
            ev.reject()
        }
    }

    override fun onEnter(ev: PointerEvent) = onHover(ev)
    override fun onExit(ev: PointerEvent) {
        if (hovering) {
            hovering = false
            hoverAnimator.start(0F)
        } else {
            ev.reject()
        }
    }
}