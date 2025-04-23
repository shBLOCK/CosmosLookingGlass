package platform

import App
import de.fabmax.kool.KoolContext
import de.fabmax.kool.KoolSystem
import de.fabmax.kool.input.PointerInput

fun launchApp(ctx: KoolContext) {
    check(ctx == KoolSystem.getContextOrNull()) { "Context should be default context: $ctx != ${KoolSystem.getContextOrNull()}" }

    RenderBackendEx.init(ctx)

    PointerInput.isEvaluatingCompatGestures = false

    App().launch()
}