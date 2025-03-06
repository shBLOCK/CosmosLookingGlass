package entry

import de.fabmax.kool.KoolApplication
import de.fabmax.kool.KoolConfigJvm

/**
 * JVM main function / app entry point: Creates a new KoolContext (with optional platform-specific configuration) and
 * forwards it to the common-code launcher.
 */
fun main() = KoolApplication(
    config = KoolConfigJvm(
        windowTitle = "kool Template App"
    )
) {
    launchApp(ctx)
}