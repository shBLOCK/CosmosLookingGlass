package platform

import de.fabmax.kool.Assets

actual fun Assets.platformImg(path: String, lossless: Boolean): String =
    if (lossless) "$path.png" else "$path.jpg"