package platform

import de.fabmax.kool.KoolApplication
import de.fabmax.kool.KoolConfigJs
import de.fabmax.kool.pipeline.backend.webgpu.GPUPowerPreference
import de.fabmax.kool.util.logD
import de.fabmax.kool.util.logE
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.await
import platform.wechat.WxAssetLoader
import kotlin.js.Promise

private const val CANVAS_NAME = "glCanvas"

internal fun isWeChatEnv() = js("(typeof wx) === 'object'") as Boolean

private fun launch() = KoolApplication(
    config = KoolConfigJs(
        defaultAssetLoader = WxAssetLoader("src/assets", "assets"),
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

@Suppress("MayBeConstant")
object Wx {
    val wx: dynamic = js("wx")
    var page: dynamic = null
        internal set

    val CLOUD_ENV_ID = "cloud1-1gkoield09107fa5"
    val CLOUD_STORAGE_ID = "636c-cloud1-1gkoield09107fa5-1357442844"

    private val cloudInitPromise: Promise<*> by lazy {
        val promise = wx.cloud.init(jsObj {
            env = CLOUD_ENV_ID
            traceUser = true
        })
        promise.then { logD { "wx.cloud.init() success" } }
        promise.catch { e ->
            logE { "wx.cloud.init() failed: $e" }
            console.error("wx.cloud.init() failed", e)
        }
        promise
    }

    suspend fun cloud(): dynamic? {
        try {
            cloudInitPromise.await()
            return wx.cloud
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            return null
        }
    }
}

internal fun weChatMain() {
    //region document
    val documentObj = jsObj {
        getElementById = fun(id: String) = when (id) {
            CANVAS_NAME -> WxGlobals.canvas
            else -> null
        }

        @Suppress("unused")
        addEventListener = fun(name: String, handler: (dynamic) -> dynamic) {}

        createElement = fun(localName: String): dynamic {
            if (localName == "canvas") {
                val canvas = Wx.wx.createOffscreenCanvas(jsObj { type = "2d" })
                canvas.style = jsObj { }
                return canvas
            }
            throw NotImplementedError("createElement '$localName'")
        }
    }

    // region keyboard
    @Suppress("unused")
    class WxKeyboardEvent(event: dynamic) {
        //@formatter:off
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
        //@formatter:on

        @JsName("getModifierState")
        fun getModifierState(keyArg: String) = when (keyArg) {
            "Alt" -> altKey
            "Shift" -> shiftKey
            else -> false
        }

        @JsName("preventDefault")
        fun preventDefault() {
        }
    }
    globalThis.KeyboardEvent = WxKeyboardEvent(jsObj { }).jsConstructor
    jsDefineProperty(
        documentObj,
        "onkeydown",
        { null.asDynamic() },
        { Wx.wx.onKeyDown { event -> it(WxKeyboardEvent(event)) } }
    )
    jsDefineProperty(
        documentObj,
        "onkeyup",
        { null.asDynamic() },
        { Wx.wx.onKeyUp { event -> it(WxKeyboardEvent(event)) } }
    )
    //endregion

    js("document = documentObj")
    //endregion

    // Force createDefaultDispatcher() to not choose a window-based dispatcher
    js("window = undefined")
    Dispatchers.Default

    //region window
    val windowObj = jsObj {
        @Suppress("unused")
        addEventListener = fun(name: String, handler: (dynamic) -> dynamic) {}

        var animationFrameBeginningTimestamp: Double? = null
        requestAnimationFrame = fun(handler: (Double) -> Unit) {
            WxGlobals.canvas.requestAnimationFrame { t: Double ->
                if (animationFrameBeginningTimestamp == null) animationFrameBeginningTimestamp = t
                handler(t - animationFrameBeginningTimestamp + 1)
            }
        }

        devicePixelRatio = Wx.wx.getWindowInfo().pixelRatio
    }

    console.log("window.devicePixelRatio: ${windowObj.devicePixelRatio}")
    jsDefineProperty(windowObj, "innerWidth") { Wx.wx.getWindowInfo().windowWidth }
    jsDefineProperty(windowObj, "innerHeight") { Wx.wx.getWindowInfo().windowHeight }
    console.log("window size: (${windowObj.innerWidth}, ${windowObj.innerHeight})")
    jsDefineProperty(
        windowObj,
        "onfocus",
        { null.asDynamic() },
        { Wx.wx.onAppShow { _ -> it(jsObj { }) } }
    )
    jsDefineProperty(
        windowObj,
        "onblur",
        { null.asDynamic() },
        { Wx.wx.onAppHide { _ -> it(jsObj { }) } }
    )
    js("window = windowObj")
    //endregion

    //region navigator
    @Suppress("UNUSED_VARIABLE", "unused")
    val navigatorObj = jsObj {
        getGamepads = { null }
    }
    js("navigator = navigatorObj")
    //endregion

    //region performance
    if (js("typeof performance") == "undefined") // overriding only if necessary: causes freeze in dev tool
        globalThis.performance = Wx.wx.getPerformance()
    //endregion

    //region mouse & touch event
    class WxMouseEvent(
        @JsName("buttons") val buttons: Int,
        @JsName("clientX") val clientX: Double,
        @JsName("clientY") val clientY: Double,
        @JsName("movementX") val movementX: Double,
        @JsName("movementY") val movementY: Double
    ) {
        constructor(pointerEvent: dynamic) : this(
            buttons = pointerEvent.detail.buttons,
            clientX = pointerEvent.detail.clientX,
            clientY = pointerEvent.detail.clientY,
            movementX = pointerEvent.detail.movementX,
            movementY = pointerEvent.detail.movementY
        )
    }
    globalThis.MouseEvent = WxMouseEvent(0, 0.0, 0.0, 0.0, 0.0).jsConstructor

    @Suppress("unused")
    class WxTouch(
        @JsName("identifier") val identifier: Int,
        @JsName("pageX") val pageX: Double,
        @JsName("pageY") val pageY: Double,
        @JsName("clientX") val clientX: Double,
        @JsName("clientY") val clientY: Double,
        @JsName("canvasX") val canvasX: Double,
        @JsName("canvasY") val canvasY: Double
    ) {
        constructor(touch: dynamic) : this(
            identifier = touch.identifier.unsafeCast<Int>(),
            pageX = touch.pageX.unsafeCast<Double>(),
            pageY = touch.pageY.unsafeCast<Double>(),
            clientX = touch.clientX.unsafeCast<Double>(),
            clientY = touch.clientY.unsafeCast<Double>(),
            canvasX = touch.x.unsafeCast<Double>(),
            canvasY = touch.y.unsafeCast<Double>()
        )
    }
    globalThis.Touch = WxTouch(0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0).jsConstructor

    @Suppress("unused")
    class WxTouchList(private val _array: Array<WxTouch>) {
        @JsName("length")
        val length = _array.size

        @JsName("item")
        fun item(index: Int): WxTouch = _array[index]
    }
    globalThis.TouchList = WxTouchList(arrayOf()).jsConstructor

    fun nativeToWxTouchList(touchList: dynamic) =
        WxTouchList(touchList.map { touch -> WxTouch(touch) }.unsafeCast<Array<WxTouch>>())

    @Suppress("unused")
    class WxTouchEvent(
        @JsName("changedTouches") val changedTouches: WxTouchList,
        @JsName("touches") val touches: WxTouchList
    ) {
        @Suppress("UnsafeCastFromDynamic")
        constructor(event: dynamic) : this(
            nativeToWxTouchList(event.changedTouches),
            nativeToWxTouchList(event.touches)
        )

        @JsName("preventDefault")
        fun preventDefault() {
        }
    }
    @Suppress("UnsafeCastFromDynamic")
    globalThis.TouchEvent = WxTouchEvent(null.asDynamic(), null.asDynamic()).jsConstructor

    @Suppress("unused")
    class WxWheelEvent(
        @JsName("deltaX") val deltaX: Double,
        @JsName("deltaY") val deltaY: Double,
        @JsName("deltaZ") val deltaZ: Double,
        @JsName("deltaMode") val deltaMode: Int
    ) {
        constructor(event: dynamic) : this(
            event.detail.deltaX,
            event.detail.deltaY,
            event.detail.deltaZ,
            event.detail.deltaMode
        )

        @JsName("preventDefault")
        fun preventDefault() {
        }
    }
    globalThis.WheelEvent = WxWheelEvent(0.0, 0.0, 0.0, 0).jsConstructor
    //endregion

    globalThis.AudioContext = Wx.wx.createWebAudioContext().jsConstructor // TODO: untested

    Wx.wx.onAfterPageLoad { res ->
        Wx.page = res.page
    }

    js("Page")(jsObj page@{
        onLoad = {
            queryWxElementById("tmp2dCanvas") {
                it.style = jsObj { } // dummy
                WxGlobals.tmp2dCanvas = it

                // inject CanvasRenderingContext2D class
                globalThis.CanvasRenderingContext2D =
                    it.getContext("2d").constructor
            }

            queryWxElementById(CANVAS_NAME) { canvas ->
                WxGlobals.canvas = canvas

                canvas.style = jsObj { } // dummy
                canvas.addEventListener = fun(name: String, handler: (dynamic) -> dynamic) {
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
                globalThis.WebGL2RenderingContext = newJsProxy(tmpCtx.constructor) {
                    get = fun(obj: dynamic, prop: dynamic): dynamic {
                        if (js("prop in obj")) return obj[prop]
                        // obj.constructor doesn't include class attributes on Android
                        return tmpCtx.__proto__[prop]
                    }
                }
                // inject WebGLRenderingContext class
                globalThis.WebGLRenderingContext = globalThis.WebGL2RenderingContext

                // inject HTMLCanvasElement class
                globalThis.HTMLCanvasElement =
                    canvas.constructor

                canvas.getContext = newJsProxy(canvas.getContext) {
                    apply = { target: dynamic, thisArg: dynamic, argumentsList: dynamic ->
                        val context = target.apply(thisArg, argumentsList)

//                        KEEP: log all GL calls, useful for debugging!
//                        js("Object").getOwnPropertyNames(context.__proto__).unsafeCast<Array<String>>()
//                            .forEach { name ->
//                                val org = context[name]
//                                if (jsTypeOf(org) == "function") {
//                                    console.log("wrapping gl context.${org.name}")
//                                    context[name] = newJsProxy(context[name]) {
//                                        apply = { target: dynamic, thisArg: dynamic, argumentsList: dynamic ->
//                                            val result = target.apply(thisArg, argumentsList)
//                                            console.log("gl: context.${org.name}", argumentsList, "->", result)
//                                            result
//                                        }
//                                    }
//                                }
//                            }

                        // workaround for https://developers.weixin.qq.com/community/develop/doc/0008a27eb80d58e1d7f252b6f6ac00
                        context.getProgramParameter = newJsProxy(context.getProgramParameter) {
                            apply = { target: dynamic, thisArg: dynamic, argumentsList: dynamic ->
                                val result = target.apply(thisArg, argumentsList)
                                val stat = argumentsList[1]
                                when (stat) {
                                    context.LINK_STATUS -> js("Boolean")(result)
                                    else -> result
                                }
                            }
                        }

                        context
                    }
                }

                // only for canvas.onmousemove handler
                canvas.getBoundingClientRect = { jsObj { left = 0.0; top = 0.0 } }

                // FINALLY launch the application...
                launch()
            }
        }

        handlePointerMove = { event: dynamic -> WxGlobals.canvas.onmousemove(WxMouseEvent(event)) }
        handlePointerDown = { event: dynamic -> WxGlobals.canvas.onmousedown(WxMouseEvent(event)) }
        handlePointerUp = { event: dynamic -> WxGlobals.canvas.onmouseup(WxMouseEvent(event)) }

        _handleTouchStart = { _: dynamic -> } // dummy
        _handleTouchMove = { _: dynamic -> } // dummy
        _handleTouchCancel = { _: dynamic -> } // dummy
        _handleTouchEnd = { _: dynamic -> } // dummy

        handleTouchStart = { event: dynamic -> this@page._handleTouchStart(event) }
        handleTouchMove = { event: dynamic -> this@page._handleTouchMove(event) }
        handleTouchCancel = { event: dynamic -> this@page._handleTouchCancel(event) }
        handleTouchEnd = { event: dynamic -> this@page._handleTouchEnd(event) }

        handleWheel = { event: dynamic -> WxGlobals.canvas.onwheel(WxWheelEvent(event)) }
    })
}

private fun queryWxElementById(id: String, handler: (dynamic) -> Unit) {
    Wx.wx.createSelectorQuery()
        .select("#$id")
        .node { res -> handler(res.node) }
        .exec()
}