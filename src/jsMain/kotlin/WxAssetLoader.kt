import de.fabmax.kool.*
import de.fabmax.kool.math.Vec2i
import de.fabmax.kool.pipeline.TexFormat
import de.fabmax.kool.platform.ImageTextureData
import kotlinx.coroutines.CompletableDeferred
import org.w3c.dom.HTMLImageElement
import org.w3c.dom.ImageBitmap
import org.w3c.dom.ImageData

class WxAssetLoader(val basePath: String) : AssetLoader() {
    override suspend fun loadBlob(ref: AssetRef.Blob): LoadedAsset.Blob {
        throw NotImplementedError("loadBlob")
//        val url = ref.path
//        val prefixedUrl = if (Assets.isHttpAsset(url)) url else "${basePath}/$url"
//        val result = fetchData(prefixedUrl).map { Uint8BufferImpl(Uint8Array(it.arrayBuffer().await())) }
//        return LoadedAsset.Blob(ref, result)
    }

    override suspend fun loadImage2d(ref: AssetRef.Image2d): LoadedAsset.Image2d {
        val resolveSz = ref.props?.resolveSize
        val result = loadImageData(ref.path, ref.isHttp, resolveSz).map {
            ImageTextureData(
                it.unsafeCast<ImageBitmap>(),
                trimAssetPath(ref.path),
                ref.props?.format ?: TexFormat.RGBA
            )
        }
        return LoadedAsset.Image2d(ref, result)
    }

    override suspend fun loadImageAtlas(ref: AssetRef.ImageAtlas): LoadedAsset.ImageAtlas {
        throw NotImplementedError("loadImageAtlas")
//        val resolveSz = ref.props?.resolveSize
//        val result = loadImageBitmap(ref.path, ref.isHttp, resolveSz).map {
//            ImageAtlasTextureData(
//                it,
//                ref.tilesX,
//                ref.tilesY,
//                trimAssetPath(ref.path),
//                ref.props?.format ?: TexFormat.RGBA
//            )
//        }
//        return LoadedAsset.ImageAtlas(ref, result)
    }

    override suspend fun loadBufferedImage2d(ref: AssetRef.BufferedImage2d): LoadedAsset.BufferedImage2d {
        throw NotImplementedError("loadBufferedImage2d")
//        val props = ref.props ?: TextureProps()
//        val texRef = AssetRef.Image2d(ref.path, props)
//        val result = loadImage2d(texRef).result.mapCatching {
//            val texData = it as ImageTextureData
//            BufferedImageData2d(
//                ImageTextureData.imageBitmapToBuffer(texData.data, props),
//                texData.width,
//                texData.height,
//                props.format,
//                trimAssetPath(ref.path)
//            )
//        }
//        return LoadedAsset.BufferedImage2d(ref, result)
    }

    override suspend fun loadAudio(ref: AssetRef.Audio): LoadedAsset.Audio {
        throw NotImplementedError("loadAudio")
//        val assetPath = ref.path
//        val clip = if (Assets.isHttpAsset(assetPath)) {
//            AudioClipImpl(assetPath)
//        } else {
//            AudioClipImpl("${basePath}/$assetPath")
//        }
//        return LoadedAsset.Audio(ref, Result.success(clip))
    }

    private suspend fun loadImageData(path: String, isHttp: Boolean, resize: Vec2i?): Result<ImageData> {
        val mime = MimeType.forFileName(path)
        val prefixedUrl = if (isHttp) path else "${basePath}/${path}"

        return if (mime != MimeType.IMAGE_SVG) {
            // raster image type -> fetch blob and create ImageBitmap directly
            val deferred = CompletableDeferred<ImageData>()
            val canvas = js("wx").createOffscreenCanvas(jsObj {type = "2d"})
            val img = canvas.createImage().unsafeCast<HTMLImageElement>()
            img.onload = {
                val size = resize ?: Vec2i(img.width, img.height)
                canvas.width = size.x
                canvas.height = size.y
                val canvasCtx = canvas.getContext("2d")
                canvasCtx.drawImage(img, 0, 0, size.x, size.y)
                deferred.complete(canvasCtx.getImageData(0, 0, size.x, size.y).unsafeCast<ImageData>())
            }
            img.onerror = { _, _, _, _, _ ->
                deferred.completeExceptionally(IllegalStateException("Failed loading tex from $prefixedUrl"))
            }
            img.src = prefixedUrl

            try {
                Result.success(deferred.await())
            } catch (t: Throwable) {
                Result.failure(t)
            }
        } else {
            // svg image -> use an Image element to convert it to an ImageBitmap
            throw NotImplementedError("loadImageBitmap: svg")
        }
    }

//    private suspend fun fetchData(path: String): Result<Response> {
//        val response = fetch(path).await()
//        return if (!response.ok) {
//            logE { "Failed loading resource $path: ${response.status} ${response.statusText}" }
//            Result.failure(IllegalStateException("Failed loading resource $path: ${response.status} ${response.statusText}"))
//        } else {
//            Result.success(response)
//        }
//    }
}

// AssetLoader.kt trimAssetPath()
private fun trimAssetPath(assetPath: String): String {
    return if (assetPath.startsWith("data:", true)) {
        val idx = assetPath.indexOf(';')
        assetPath.substring(0 until idx)
    } else {
        assetPath
    }
}