@file:Suppress("NOTHING_TO_INLINE")

package ui.hud

import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.util.Color
import ui.FreeLayout
import ui.KeyedStateStore
import ui.keyed
import ui.withKey
import universe.CelestialBody
import universe.Universe
import universe.globalOutlineSphere
import utils.curry
import utils.mix
import utils.projectSphereViewport
import utils.toFloat

class UniverseCelestialBodyHudButtons(private val universe: Universe) : Composable, KeyedStateStore.Managed {
    var configurator: HudOutlineSphereButtonNode.(CelestialBody) -> Unit = {}
    var onClick: ((CelestialBody) -> Unit)? = null
    var onDoubleClick: ((CelestialBody) -> Unit)? = null

    override val stateStore = KeyedStateStore()
    private val buttons = mutableListOf<HudOutlineSphereButtonNode>()

    override fun UiScope.compose() = stateStore.cycle {
        check(uiNode.modifier.layout === FreeLayout)

        buttons.clear()
        universe.forEach { cb ->
            val proj = universe.view.projectSphereViewport(cb.globalOutlineSphere)
            if (proj.centerDistance > 0.0) withKey(cb) {
                val button = uiNode.createChild(
                    "CelestialBodyHudButton(${cb.name})",
                    HudOutlineSphereButtonNode::class,
                    ::HudOutlineSphereButtonNode.curry(stateStore)
                ).apply {
                    modifier.color = cb.themeColor
                    modifier.onButtonClick = { onClick?.invoke(cb) }
                    modifier.onButtonDoubleClick = { onDoubleClick?.invoke(cb) }
                    setup(proj)
                    configurator(cb)
                }
                buttons += button

                val labelHoverAnimation = keyed { AnimatedFloatBidir(0.2F) }
                val labelHideAnimation = keyed { AnimatedFloatBidir(0.2F) }

                HudAnnotation(button.modifier.freePos) {
                    modifier.margin(button.modifier.radius + 5.dp)

                    Text(cb.name) {
                        modifier
                            .font(sizes.largeText)

                        modifier.onMeasured {
                            labelHoverAnimation.start(button.hovered.toFloat())
                            labelHideAnimation.start(button.hidden.toFloat())
                            if (button.projection.centerDistance < 0.0)
                                labelHideAnimation.set(1F)

                            var alpha = mix(0.75F, 1F, labelHoverAnimation.progressAndUse())
                            alpha *= 1F - labelHideAnimation.progressAndUse()
                            modifier.textColor(Color.LIGHT_GRAY.withAlpha(alpha))
                        }
                    }
                }
            }
        }

        // sort by priority high-to-low then depth near-to-far
        buttons.sortWith { a, b ->
            b.modifier.priority.compareTo(a.modifier.priority).takeIf { it != 0 }
                ?: a.projection.centerDistance.compareTo(b.projection.centerDistance)
        }

        // hide buttons to resolve overlaps
        // in the now sorted buttons list, a button can be occluded by buttons in front of it
        // already hidden buttons are ignored
        btn@ for ((i, btn) in buttons.withIndex()) {
            if (btn.hidden) continue
            for (occludingBtn in buttons.subList(0, i)) {
                if (occludingBtn.hidden) continue
                if (occludingBtn.overlaps(btn)) {
                    btn.hidden = true
                    continue@btn
                }
            }
        }
    }
}