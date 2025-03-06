import de.fabmax.kool.KoolApplication
import de.fabmax.kool.KoolConfigJs
import de.fabmax.kool.pipeline.backend.webgpu.GPUPowerPreference
import template.launchApp

private const val CANVAS_NAME = "glCanvas"
private const val ASSETS_ROOT = "./src/assets"

fun main() = if (!isWeChatEnv()) launch() else weChatMain()

private fun launch() {
    KoolApplication(
        config = KoolConfigJs(
            defaultAssetLoader = WxAssetLoader(ASSETS_ROOT),
            canvasName = CANVAS_NAME,
            isJsCanvasToWindowFitting = true,
            renderBackend = KoolConfigJs.Backend.WEB_GL2,
            powerPreference = GPUPowerPreference.highPerformance
        )
    ) {
        launchApp(ctx)
    }
}

private fun isWeChatEnv() = js("(typeof wx) === 'object'") as Boolean

private object Globals {
    var canvas: dynamic = null
    var tmp2dCanvas: dynamic = null
}

private fun weChatMain() {
    //region document
    @Suppress("UNUSED_VARIABLE")
    val documentObj = jsObj {
        getElementById = fun(id: String) = when (id) {
            CANVAS_NAME -> Globals.canvas
            else -> null
        }

        addEventListener = fun(name: String, handler: (dynamic) -> dynamic) {
            //TODO
        }

        createElement = fun(localName: String): dynamic {
            if (localName == "canvas") {
                val canvas = js("wx").createOffscreenCanvas(jsObj { type = "2d" })
                canvas.style = jsObj { }
                return canvas
            }
            throw NotImplementedError("createElement '$localName'")
        }
    }
    js("document = documentObj")
    //endregion

    //region window
    val windowObj = jsObj {
        addEventListener = fun(name: String, handler: (dynamic) -> dynamic) {
            //TODO
        }

        requestAnimationFrame = fun(handler: dynamic) {
            Globals.canvas.requestAnimationFrame(handler)
        }
    }
    jsDefineProperty(windowObj, "devicePixelRatio") { js("wx").getWindowInfo().pixelRatio }
    jsDefineProperty(windowObj, "innerWidth") { js("wx").getWindowInfo().windowWidth }
    jsDefineProperty(windowObj, "innerHeight") { js("wx").getWindowInfo().windowHeight }
    js("window = windowObj")
    //endregion

    //region navigator
    @Suppress("UNUSED_VARIABLE")
    val navigatorObj = jsObj {
        getGamepads = { null }
    }
    js("navigator = navigatorObj")
    //endregion

    //region performance
    if (js("typeof performance") == "undefined") // overriding only if necessary: causes freeze in dev tool
        globalThis.performance = js("wx").getPerformance()
    //endregion

//    js("require('extra_wechat_compat_hacks.js')")

    js("Page")(jsObj {
        onLoad = {
            queryElementById("tmp2dCanvas") {
                it.style = jsObj { } // dummy
                Globals.tmp2dCanvas = it

                // inject CanvasRenderingContext2D class
                globalThis.CanvasRenderingContext2D =
                    it.getContext("2d").__proto__.constructor
            }

            queryElementById(CANVAS_NAME) { canvas ->
                Globals.canvas = canvas

                canvas.style = jsObj { } // dummy
                canvas.addEventListener = fun(name: String, handler: (dynamic) -> dynamic) {
                    //TODO
                }

                val tmpCtx = canvas.getContext("webgl2")
                console.log("WebGL2 supported extensions: ${tmpCtx.getSupportedExtensions()}")

                // inject WebGL2RenderingContext class
                globalThis.WebGL2RenderingContext =
                    tmpCtx.__proto__.constructor
                // inject WebGLRenderingContext class
                globalThis.WebGLRenderingContext = globalThis.WebGL2RenderingContext

                // inject HTMLCanvasElement class
                globalThis.HTMLCanvasElement =
                    canvas.__proto__.constructor

                // FINALLY launch the application...
                launch()
            }
        }
    })
}

fun jsObj(block: dynamic.() -> Unit): dynamic {
    val obj = js("{}")
    block(obj)
    return obj
}

fun <T> jsDefineProperty(obj: dynamic, prop: String, getter: () -> T, setter: ((T) -> dynamic)? = null) {
    js("Object").defineProperty(obj, prop, jsObj {
        get = getter
        setter?.also { set = it }
    })
}

fun <T> jsDefineProperty(obj: dynamic, prop: String, getter: () -> T) =
    jsDefineProperty(obj, prop, getter, null)

private external val globalThis: dynamic

private fun queryElementById(id: String, handler: (dynamic) -> Unit) {
    js("wx").createSelectorQuery().select("#$id").node { res ->
        handler(res.node)
    }.exec()
}

//@Suppress("UnsafeCastFromDynamic")
//private fun fetchImpl(url: String): Promise<Response> {
//    if (url.startsWith(ASSETS_ROOT)) {
//        return Promise { resolve, reject ->
//            js("wx").getFileSystemManager().readFile(jsObj {
//                filePath = url
//            })
//        }
//    } else {
//        throw NotImplementedError(url)
//    }
//    Promise { resolve, reject ->
//        js("wx").request(jsObj {
//            this.url = url
//            method = "GET"
//            success = { res: dynamic ->
//                resolve(jsObj {
//                    ok = true
//                    status = res.
//                })
//            }
//            fail = { err: dynamic ->
//                reject(js("Error")(jsObj {
//                    ok = false
//                    status = err.errno
//                    statusText = err.errMsg
//                }))
//            }
//        })
//        Unit
//    }
//}