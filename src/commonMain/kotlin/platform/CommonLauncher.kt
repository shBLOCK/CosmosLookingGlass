package platform

import de.fabmax.kool.KoolContext
import de.fabmax.kool.input.PointerInput
import de.fabmax.kool.math.Vec3d
import de.fabmax.kool.math.deg
import de.fabmax.kool.util.DebugOverlay
import de.fabmax.kool.util.debugOverlay
import dynamics.SolarSystemKeplerModel3000BC3000AD
import dynamics.transformed
import ui.MainUI
import universe.content.SolarSystem

fun launchApp(ctx: KoolContext) {
    ctx.initRenderBackendEx()

    PointerInput.isEvaluatingCompatGestures = false

    val solarSystem = SolarSystem().apply {
        dynModel = SolarSystemKeplerModel3000BC3000AD()
            .transformed { time, position, orientation -> // to ICRF
                position.rotate(23.43928.deg, Vec3d.X_AXIS)
                orientation.rotate(23.43928.deg, Vec3d.X_AXIS)
            }
    }
    ctx.scenes += solarSystem.scene
    ctx.scenes += MainUI(solarSystem).scene

    ctx.scenes += debugOverlay(DebugOverlay.Position.LOWER_RIGHT)
}