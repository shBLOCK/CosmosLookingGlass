package platform

fun jsObj(block: dynamic.() -> Unit): dynamic {
    val obj = js("({})")
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

@Suppress("UnusedVariable")
fun newJsProxy(obj: dynamic, optionsConfigurator: dynamic.() -> Unit): dynamic {
    val options = jsObj(optionsConfigurator)
    return js("new Proxy(obj, options)")
}

val Any?.jsConstructor get() = asDynamic().constructor

external val globalThis: dynamic