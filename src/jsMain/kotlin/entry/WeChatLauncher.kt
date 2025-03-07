package entry

import de.fabmax.kool.KoolApplication
import de.fabmax.kool.KoolConfigJs
import de.fabmax.kool.pipeline.backend.webgpu.GPUPowerPreference
import wechat.WxAssetLoader
import utils.jsObj
import utils.jsDefineProperty
import utils.globalThis
import utils.jsClass

private const val CANVAS_NAME = "glCanvas"
private const val ASSETS_ROOT = "./src/assets"

internal fun isWeChatEnv() = js("(typeof wx) === 'object'") as Boolean

private fun launch() = KoolApplication(
    config = KoolConfigJs(
        defaultAssetLoader = WxAssetLoader(ASSETS_ROOT),
        canvasName = CANVAS_NAME,
        isJsCanvasToWindowFitting = true,
        isGlobalKeyEventGrabbing = true, // bind event handlers on document
        renderBackend = KoolConfigJs.Backend.WEB_GL2,
        powerPreference = GPUPowerPreference.highPerformance
    )
) {
    launchApp(ctx)
}

private object WxGlobals {
    var canvas: dynamic = null
    var tmp2dCanvas: dynamic = null
}


internal fun weChatMain() {
    //region document
    val documentObj = jsObj {
        getElementById = fun(id: String) = when (id) {
            CANVAS_NAME -> WxGlobals.canvas
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

    // region keyboard
    @Suppress("unused")
    class WxKeyboardEvent(event: dynamic) {
        @JsName("key") val key = event.key.unsafeCast<String>()
        @JsName("code") val code = event.code.unsafeCast<String>()
        @JsName("altKey") val altKey = event.altKey.unsafeCast<Boolean>()
        @JsName("shiftKey") val shiftKey = event.shiftKey.unsafeCast<Boolean>()
        @JsName("timeStamp") val timeStamp = event.timeStamp.unsafeCast<Double>()

        @JsName("location") val location = 0x00 // DOM_KEY_LOCATION_STANDARD
        @JsName("ctrlKey") val ctrlKey = false
        @JsName("metaKey") val metaKey = false
        @JsName("repeat") val repeat = false
        @JsName("isComposing") val isComposing = false

        @JsName("getModifierState")
        fun getModifierState(keyArg: String) = when (keyArg) {
            "Alt" -> altKey
            "Shift" -> shiftKey
            else -> false
        }
    }
    globalThis.KeyboardEvent = WxKeyboardEvent(jsObj { }).jsClass
    jsDefineProperty(
        documentObj,
        "onkeydown",
        { null.asDynamic() },
        { js("wx").onKeyDown { event -> it(WxKeyboardEvent(event)) } }
    )
    jsDefineProperty(
        documentObj,
        "onkeyup",
        { null.asDynamic() },
        { js("wx").onKeyUp { event -> it(WxKeyboardEvent(event)) } }
    )
    //endregion

    js("document = documentObj")
    //endregion

    //region window
    val windowObj = jsObj {
        addEventListener = fun(name: String, handler: (dynamic) -> dynamic) {
            //TODO
        }

        requestAnimationFrame = fun(handler: dynamic) {
            WxGlobals.canvas.requestAnimationFrame(handler)
        }

        devicePixelRatio = js("wx").getWindowInfo().pixelRatio
    }
//    jsDefineProperty(windowObj, "devicePixelRatio") { js("wx").getWindowInfo().pixelRatio }
    console.log("window.devicePixelRatio: ${windowObj.devicePixelRatio}")
    jsDefineProperty(windowObj, "innerWidth") { js("wx").getWindowInfo().windowWidth }
    jsDefineProperty(windowObj, "innerHeight") { js("wx").getWindowInfo().windowHeight }
    console.log("window size: (${windowObj.innerWidth}, ${windowObj.innerHeight})")
    jsDefineProperty(
        windowObj,
        "onfocus",
        { null.asDynamic() },
        { js("wx").onAppShow { _ -> it(jsObj { }) } }
    )
    jsDefineProperty(
        windowObj,
        "onblur",
        { null.asDynamic() },
        { js("wx").onAppHide { _ -> it(jsObj { }) } }
    )
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

    @Suppress("unused")
    class WxTouch(
        @JsName("identifier") val identifier: Int,
        pPageX: Double,
        pPageY: Double,
        pClientX: Double,
        pClientY: Double,
        pCanvasX: Double,
        pCanvasY: Double
    ) {
        constructor(touch: dynamic) : this(
            identifier = touch.identifier.unsafeCast<Int>(),
            pPageX = touch.pageX.unsafeCast<Double>(),
            pPageY = touch.pageY.unsafeCast<Double>(),
            pClientX = touch.clientX.unsafeCast<Double>(),
            pClientY = touch.clientY.unsafeCast<Double>(),
            pCanvasX = touch.x.unsafeCast<Double>(),
            pCanvasY = touch.y.unsafeCast<Double>()
        )

        @JsName("clientX") val clientX = pClientX
        @JsName("clientY") val clientY = pClientY
    }
    globalThis.Touch = WxTouch(0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0).jsClass

    @Suppress("unused")
    class WxTouchList(private val _array: Array<WxTouch>) {
        constructor(touchList: dynamic) : this(touchList.map { touch -> WxTouch(touch) }.unsafeCast<Array<WxTouch>>())

        @JsName("length") val length = _array.size
        @JsName("item") fun item(index: Int): WxTouch = _array[index]
    }
    globalThis.TouchList = WxTouchList(arrayOf()).jsClass

    @Suppress("unused")
    class WxTouchEvent(
        @JsName("changedTouches") val changedTouches: WxTouchList,
        @JsName("touches") val touches: WxTouchList
    ) {
        @Suppress("UnsafeCastFromDynamic")
        constructor(event: dynamic) : this(
            WxTouchList(event.changedTouches),
            WxTouchList(event.touches)
        ) {
            console.log(event)
        }

        @JsName("preventDefault")
        fun preventDefault() {}
    }
    @Suppress("UnsafeCastFromDynamic")
    globalThis.TouchEvent = WxTouchEvent(null.asDynamic(), null.asDynamic()).jsClass

    js("Page")(jsObj page@{
        onLoad = {
            queryWxElementById("tmp2dCanvas") {
                it.style = jsObj { } // dummy
                WxGlobals.tmp2dCanvas = it

                // inject CanvasRenderingContext2D class
                globalThis.CanvasRenderingContext2D =
                    it.getContext("2d").__proto__.constructor
            }

            queryWxElementById(CANVAS_NAME) { canvas ->
                WxGlobals.canvas = canvas

                canvas.style = jsObj { } // dummy
                canvas.addEventListener = fun(name: String, handler: (dynamic) -> dynamic) {
                    console.log("canvas.$name: $handler")
                    when (name) {
                        "touchstart" -> this@page._handleTouchStart = { e: dynamic -> handler(WxTouchEvent(e)) }
                        "touchmove" -> this@page._handleTouchMove = { e: dynamic -> handler(WxTouchEvent(e)) }
                        "touchcancel" -> this@page._handleTouchCancel = { e: dynamic -> handler(WxTouchEvent(e)) }
                        "touchend" -> this@page._handleTouchEnd = { e: dynamic -> handler(WxTouchEvent(e)) }
                    }
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

        _handleTouchStart = { _: dynamic -> } // dummy
        _handleTouchMove = { _: dynamic -> } // dummy
        _handleTouchCancel = { _: dynamic -> } // dummy
        _handleTouchEnd = { _: dynamic -> } // dummy

        handleTouchStart = { event: dynamic -> this@page._handleTouchStart(event) }
        handleTouchMove = { event: dynamic -> this@page._handleTouchMove(event) }
        handleTouchCancel = { event: dynamic -> this@page._handleTouchCancel(event) }
        handleTouchEnd = { event: dynamic -> this@page._handleTouchEnd(event) }
    })
}

private fun queryWxElementById(id: String, handler: (dynamic) -> Unit) {
    js("wx").createSelectorQuery().select("#$id").node { res ->
        handler(res.node)
    }.exec()
}