package platform

import de.fabmax.kool.KoolContext
import de.fabmax.kool.input.PointerInput
import de.fabmax.kool.util.DebugOverlay
import de.fabmax.kool.util.debugOverlay
import ui.Hud
import ui.MainCameraControl
import universe.SolarSystemScene

fun launchApp(ctx: KoolContext) {
    ctx.initRenderBackendEx()

    PointerInput.isEvaluatingCompatGestures = false

    val solarSystem = SolarSystemScene().apply {
        this += MainCameraControl(mainRenderPass.defaultView)
    }.also(ctx.scenes::stageAdd)

    ctx.scenes += Hud(solarSystem).scene

    ctx.scenes += debugOverlay(DebugOverlay.Position.LOWER_RIGHT)
}