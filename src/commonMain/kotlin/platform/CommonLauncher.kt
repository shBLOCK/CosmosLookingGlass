package platform

import de.fabmax.kool.KoolContext
import de.fabmax.kool.input.PointerInput
import de.fabmax.kool.util.DebugOverlay
import de.fabmax.kool.util.debugOverlay
import ui.MainUI
import universe.SolarSystemScene

fun launchApp(ctx: KoolContext) {
    ctx.initRenderBackendEx()

    PointerInput.isEvaluatingCompatGestures = false

    val solarSystem = SolarSystemScene()
    ctx.scenes += solarSystem
    ctx.scenes += MainUI(solarSystem).scene

    ctx.scenes += debugOverlay(DebugOverlay.Position.LOWER_RIGHT)
}