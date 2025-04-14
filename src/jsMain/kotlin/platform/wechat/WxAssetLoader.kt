package platform.wechat

import de.fabmax.kool.*
import de.fabmax.kool.math.Vec2i
import de.fabmax.kool.platform.ImageTextureData
import de.fabmax.kool.util.Uint8BufferImpl
import platform.jsObj
import platform.wxPage
import kotlinx.coroutines.CompletableDeferred
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import org.w3c.dom.HTMLImageElement
import org.w3c.dom.ImageBitmap
import org.w3c.dom.ImageData

class WxAssetLoader(val basePath: String) : AssetLoader() {
    override suspend fun loadBlob(ref: AssetRef.Blob): LoadedAsset.Blob {
        val deferred = CompletableDeferred<Result<ArrayBuffer>>()
        js("wx").getFileSystemManager().readFile(jsObj {
            val pageBase = (wxPage.route as String)
                .run { if (contains('/')) substring(0..<lastIndexOf('/')) else "" }
            filePath = if (ref.isHttp) ref.path else "${pageBase}/${basePath}/${ref.path}"
            success = { res: dynamic ->
                deferred.complete(Result.success(res.data))
            }
            fail = { res: dynamic ->
                deferred.complete(Result.failure(RuntimeException("Failed to load blob $ref: ${JSON.stringify(res)}")))
            }
        })
        return LoadedAsset.Blob(ref, deferred.await().map { Uint8BufferImpl(Uint8Array(it)) })
    }

    override suspend fun loadImage2d(ref: AssetRef.Image2d): LoadedAsset.Image2d {
        val resolveSz = ref.resolveSize
        val result = loadImageData(ref.path, ref.isHttp, resolveSz).map {
            ImageTextureData(
                it.unsafeCast<ImageBitmap>(), // ImageData is mostly compatible with ImageBitmap
                trimAssetPath(ref.path),
                ref.format
            )
        }
        return LoadedAsset.Image2d(ref, result)
    }

    override suspend fun loadImageAtlas(ref: AssetRef.ImageAtlas): LoadedAsset.ImageAtlas {
        throw NotImplementedError("loadImageAtlas")
    }

    override suspend fun loadBufferedImage2d(ref: AssetRef.BufferedImage2d): LoadedAsset.BufferedImage2d {
        throw NotImplementedError("loadBufferedImage2d")
    }

    override suspend fun loadAudio(ref: AssetRef.Audio): LoadedAsset.Audio {
        throw NotImplementedError("loadAudio")
    }

    private suspend fun loadImageData(path: String, isHttp: Boolean, resize: Vec2i?): Result<ImageData> {
        val mime = MimeType.forFileName(path)
        val prefixedUrl = if (isHttp) path else "${basePath}/${path}"

        if (mime != MimeType.IMAGE_SVG) {
            // raster image type -> fetch blob and create ImageBitmap directly
            val deferred = CompletableDeferred<ImageData>()
            val canvas = js("wx").createOffscreenCanvas(jsObj { type = "2d" })
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

            return try {
                Result.success(deferred.await())
            } catch (t: Throwable) {
                Result.failure(t)
            }
        } else {
            // svg image -> use an Image element to convert it to an ImageBitmap
            throw NotImplementedError("loadImageBitmap: svg")
        }
    }
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