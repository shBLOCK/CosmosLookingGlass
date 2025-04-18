package platform

import de.fabmax.kool.KoolContext
import de.fabmax.kool.pipeline.backend.RenderBackend
import de.fabmax.kool.pipeline.backend.gl.GlImpl
import de.fabmax.kool.pipeline.backend.gl.RenderBackendGlImpl
import de.fabmax.kool.pipeline.backend.webgpu.RenderBackendWebGpu
import org.khronos.webgl.WebGLRenderingContext

class RenderBackendExWebGL(private val backend: RenderBackendGlImpl) : RenderBackendEx {
    override val maxTextureSize by lazy { GlImpl.gl.getParameter(WebGLRenderingContext.MAX_TEXTURE_SIZE) as Int }
}

@Suppress("ObjectPropertyName")
@PublishedApi
internal lateinit var _EX: RenderBackendEx

actual inline val RenderBackend.ex: RenderBackendEx get() = _EX

actual fun KoolContext.initRenderBackendEx() {
    _EX = when (val backend = backend) {
        is RenderBackendGlImpl -> RenderBackendExWebGL(backend)
        is RenderBackendWebGpu -> TODO()
        else -> error("Unknown render backend")
    }
}