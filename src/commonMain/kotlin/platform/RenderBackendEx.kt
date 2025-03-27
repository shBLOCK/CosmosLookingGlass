package platform

import de.fabmax.kool.KoolContext
import de.fabmax.kool.pipeline.backend.RenderBackend

interface RenderBackendEx {
    val maxTextureSize: Int
    val textureFloatLinear: Boolean
}

expect val RenderBackend.ex: RenderBackendEx
expect fun KoolContext.initRenderBackendEx()