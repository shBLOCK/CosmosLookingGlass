package platform

import de.fabmax.kool.KoolContext
import de.fabmax.kool.pipeline.backend.RenderBackend

interface RenderBackendEx {
    val maxTextureSize: Int

    companion object
}

expect val RenderBackend.ex: RenderBackendEx
expect fun RenderBackendEx.Companion.init(ctx: KoolContext)