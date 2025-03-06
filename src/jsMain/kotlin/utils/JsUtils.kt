package utils

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

external val globalThis: dynamic