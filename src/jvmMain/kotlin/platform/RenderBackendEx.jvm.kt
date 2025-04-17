package platform

import de.fabmax.kool.KoolContext
import de.fabmax.kool.pipeline.backend.RenderBackend
import de.fabmax.kool.pipeline.backend.gl.RenderBackendGlImpl
import de.fabmax.kool.pipeline.backend.vk.RenderBackendVk
import org.lwjgl.opengl.GL45.GL_MAX_TEXTURE_SIZE
import org.lwjgl.opengl.GL45.glGetInteger

class RenderBackendExOpenGL(private val backend: RenderBackendGlImpl) : RenderBackendEx {
    override val maxTextureSize by lazy { glGetInteger(GL_MAX_TEXTURE_SIZE) }
    override val textureFloatLinear = true
}

@Suppress("ObjectPropertyName")
@PublishedApi
internal lateinit var _EX: RenderBackendEx

actual inline val RenderBackend.ex: RenderBackendEx get() = _EX

actual fun KoolContext.initRenderBackendEx() {
    _EX = when (val backend = backend) {
        is RenderBackendGlImpl -> RenderBackendExOpenGL(backend)
        is RenderBackendVk -> TODO()
        else -> error("Unknown render backend")
    }
}