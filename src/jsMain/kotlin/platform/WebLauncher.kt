package platform

import de.fabmax.kool.KoolApplication
import de.fabmax.kool.KoolConfigJs
import de.fabmax.kool.NativeAssetLoader
import de.fabmax.kool.pipeline.backend.webgpu.GPUPowerPreference

internal fun webMain() = KoolApplication(
    config = KoolConfigJs(
        defaultAssetLoader = NativeAssetLoader("assets"),
        canvasName = "glCanvas",
        isJsCanvasToWindowFitting = true,
        renderBackend = KoolConfigJs.Backend.WEB_GL2,
        powerPreference = GPUPowerPreference.highPerformance
    )
) {
    launchApp(ctx)
}