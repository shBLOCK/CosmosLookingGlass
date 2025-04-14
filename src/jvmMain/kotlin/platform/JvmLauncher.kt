package platform

import de.fabmax.kool.KoolApplication
import de.fabmax.kool.KoolConfigJvm

fun main() {
    System.loadLibrary("renderdoc")
    KoolApplication(
        config = KoolConfigJvm(
            windowTitle = "kool Template App",
            renderBackend = KoolConfigJvm.Backend.OPEN_GL,
            classloaderAssetPath = "assets"
        )
    ) {
        launchApp(ctx)
    }
}