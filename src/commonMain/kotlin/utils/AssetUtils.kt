package utils

import de.fabmax.kool.AssetLoader
import de.fabmax.kool.math.Vec2i
import de.fabmax.kool.pipeline.BufferedImageData2d
import de.fabmax.kool.pipeline.FilterMethod
import de.fabmax.kool.pipeline.ImageDataCube
import de.fabmax.kool.pipeline.MipMapping
import de.fabmax.kool.pipeline.SamplerSettings
import de.fabmax.kool.pipeline.SingleColorTexture
import de.fabmax.kool.pipeline.TexFormat
import de.fabmax.kool.pipeline.TextureCube
import de.fabmax.kool.pipeline.toTexture
import de.fabmax.kool.util.Color

class SingleColorTextureCube(color: Color) : TextureCube(
    format = TexFormat.RGBA,
    mipMapping = MipMapping.Off,
    samplerSettings = DEFAULT_SAMPLER_SETTINGS,
    name = "SingleColorTexCube:${color}"
) {
    init {
        uploadLazy(getColorTextureData(color))
    }

    companion object {
        private val DEFAULT_SAMPLER_SETTINGS = SamplerSettings(
            minFilter = FilterMethod.NEAREST,
            magFilter = FilterMethod.NEAREST,
            maxAnisotropy = 1,
        )

        private val colorData = mutableMapOf<Color, ImageDataCube>()

        fun getColorTextureData(color: Color): ImageDataCube {
            return colorData.getOrPut(color) {
                val face = SingleColorTexture.getColorTextureData(color)
                ImageDataCube(
                    face, face, face, face, face, face,
                    id = "SingleColorDataCube[$color]"
                )
            }
        }
    }
}

suspend fun AssetLoader.loadImageCube(
    assetPath: String,
    format: TexFormat = TexFormat.RGBA,
    resolveSize: Vec2i? = null
): Result<ImageDataCube> {
    val pre = assetPath.substringBeforeLast('.')
    val post = assetPath.substringAfterLast('.')
    return loadImageCube(
        "${pre}_neg_x.${post}",
        "${pre}_pos_x.${post}",
        "${pre}_neg_y.${post}",
        "${pre}_pos_y.${post}",
        "${pre}_neg_z.${post}",
        "${pre}_pos_z.${post}",
        format, resolveSize
    )
}

suspend fun AssetLoader.loadTextureCube(
    assetPath: String,
    format: TexFormat = TexFormat.RGBA,
    mipMapping: MipMapping = MipMapping.Full,
    samplerSettings: SamplerSettings = SamplerSettings(),
    resolveSize: Vec2i? = null
): Result<TextureCube> = loadImageCube(assetPath, format, resolveSize).map {
    val name = "cubeMap($assetPath)"
    it.toTexture(mipMapping, samplerSettings, name)
}