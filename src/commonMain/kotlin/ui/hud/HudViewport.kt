@file:OptIn(ExperimentalContracts::class)
@file:Suppress("FunctionName")

package ui.hud

import de.fabmax.kool.modules.ui2.*
import ui.FreeLayout
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class HudViewport internal constructor(parent: UiNode?, surface: UiSurface) : BoxNode(parent, surface) {
    internal fun setup() {
        check(parent == surface.viewport)
        surface.inputMode = UiSurface.InputCaptureMode.CapturePassthrough
        parent!!.let { setBounds(it.leftPx, it.topPx, it.rightPx, it.bottomPx) }
        modifier
            .size(Grow.Std, Grow.Std)
            .layout(FreeLayout)
    }
}

fun UiScope.HudViewport(scopeName: String? = "hudViewport", block: HudViewport.() -> Unit) {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    uiNode.createChild(scopeName, HudViewport::class, ::HudViewport).apply {
        setup()
        block()
    }
}