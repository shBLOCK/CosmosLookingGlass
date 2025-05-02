package platform

import de.fabmax.kool.Assets

actual fun Assets.platformImg(path: String, lossless: Boolean): String =
//    "$path.webp" // TODO: wechat bug: https://developers.weixin.qq.com/community/develop/doc/0000a27e0a4bb0115bb11af8360000?highLine=webp%2520cdn https://developers.weixin.qq.com/community/develop/doc/000a24dddb0978ed5462f60556bc00?highLine=webp%2520cdn
    if (path.startsWith("cdn/")) "$path.webp" else "$path.jpg"