package platform.wechat

import de.fabmax.kool.AssetLoader
import de.fabmax.kool.AssetRef
import de.fabmax.kool.Assets
import de.fabmax.kool.LoadedAsset
import de.fabmax.kool.math.Vec2i
import de.fabmax.kool.platform.ImageTextureData
import de.fabmax.kool.util.Uint8BufferImpl
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.await
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import org.w3c.dom.HTMLImageElement
import org.w3c.dom.ImageBitmap
import org.w3c.dom.ImageData
import platform.Wx
import platform.jsObj
import kotlin.js.Promise

class WxAssetLoader(val assetsRoot: String, val cdnRoot: String) : AssetLoader() {
    override suspend fun loadBlob(ref: AssetRef.Blob): LoadedAsset.Blob {
        val deferred = CompletableDeferred<Result<ArrayBuffer>>()
        val path = transformPath(ref.path, pageLocal = false)
        Wx.wx.getFileSystemManager().readFile(jsObj {
            filePath = path
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
        val result = loadImageData(transformPath(ref.path), resolveSz).map {
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

    private suspend fun loadImageData(path: String, resize: Vec2i?): Result<ImageData> {
        val deferred = CompletableDeferred<ImageData>()
        val canvas = Wx.wx.createOffscreenCanvas(jsObj { type = "2d" })
        val img = canvas.createImage().unsafeCast<HTMLImageElement>()
        img.onload = {
            val size = resize ?: Vec2i(img.width, img.height)
            canvas.width = size.x
            canvas.height = size.y
            val canvasCtx = canvas.getContext("2d")
            canvasCtx.drawImage(img, 0, 0, size.x, size.y)
            deferred.complete(canvasCtx.getImageData(0, 0, size.x, size.y).unsafeCast<ImageData>())
        }
        img.onerror = { message, source, lineno, colno, error ->
            deferred.completeExceptionally(IllegalStateException("Failed loading tex from $path: msg=$message; src=$source; lineno=$lineno; colno=$colno; error=$error"))
        }
        img.asDynamic().webp = true // doesn't really seem to work
        img.src = path

        return try {
            Result.success(deferred.await())
        } catch (e: CancellationException) {
            throw e
        } catch (t: Exception) {
            Result.failure(t)
        }
    }

    private suspend fun transformPath(path: String, pageLocal: Boolean = true): String {
        var path = path

        if (Assets.isHttpAsset(path))
            return path

        if (path.startsWith("cdn")) {
            val cloud = checkNotNull(Wx.cloud()) { "WX cloud not available" }
            try {
                val result = (cloud.getTempFileURL(jsObj {
                    fileList = arrayOf(jsObj {
                        fileID = "cloud://${Wx.CLOUD_ENV_ID}.${Wx.CLOUD_STORAGE_ID}/$cdnRoot/$path"
                        maxAge = 24 * 3600
                    })
                }) as Promise<dynamic>).await().fileList[0]
                if (result.status !== 0)
                    throw RuntimeException("Failed to get CDN asset temp URL for $path: ${result.status} (${result.errMsg})")
                return result.tempFileURL
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                throw RuntimeException("Failed to get CDN asset temp URL for $path: $e", cause = e)
            }
        } else {
            path = "$assetsRoot/$path"
            if (!pageLocal) {
                val pageBase = (Wx.page.route as String)
                    .run { if (contains('/')) substring(0..<lastIndexOf('/')) else "" }
                path = "$pageBase/$path"
            }
        }
        return path
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