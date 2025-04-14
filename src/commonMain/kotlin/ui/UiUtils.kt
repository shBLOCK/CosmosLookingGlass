@file:Suppress("NOTHING_TO_INLINE")

package ui

import de.fabmax.kool.Assets
import de.fabmax.kool.KoolSystem
import de.fabmax.kool.loadBlob
import de.fabmax.kool.loadImage2d
import de.fabmax.kool.modules.ui2.Dp
import de.fabmax.kool.modules.ui2.MutableStateValue
import de.fabmax.kool.modules.ui2.UiSurface
import de.fabmax.kool.pipeline.MipMapping
import de.fabmax.kool.pipeline.Texture2d
import de.fabmax.kool.util.MsdfFont
import de.fabmax.kool.util.MsdfFontData
import de.fabmax.kool.util.MsdfMeta
import de.fabmax.kool.util.decodeToString
import de.fabmax.kool.util.logW
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import utils.FwdInvFunction

inline val Float.dpx get() = Dp.fromPx(this)
inline val Double.dpx get() = Dp.fromPx(this.toFloat())

class MappedStateValue<A : Any?, B : Any?>(private val source: MutableStateValue<B>, pMapper: FwdInvFunction<A, B>) :
    MutableStateValue<A>(pMapper.inverse(source.value)) {

    var function: FwdInvFunction<A, B> = pMapper
        set(value) {
            field = value
            set(value.inverse(source.value))
        }

    init {
        onChange { _, new -> source.set(function(new)) }
        source.onChange { _, new -> set(function.inverse(new)) }
    }
}

object UiUtils {
    fun triggerUpdateOnAllUiSurface() {
        KoolSystem.getContextOrNull()?.scenes?.forEach { scene ->
            scene.children.forEach { (it as? UiSurface)?.triggerUpdate() }
        }
    }
}

private suspend fun loadMsdfFontData(path: String): Result<MsdfFontData> {
    val img = Assets.loadImage2d("$path.png")
        .getOrElse { e ->
            return Result.failure(e)
        }
    val meta = Assets.loadBlob("$path.json").mapCatching {
        val json = it.decodeToString()
        Json {
            ignoreUnknownKeys = true
        }.decodeFromString<MsdfMeta>(json)
    }.getOrElse { e ->
        return Result.failure(e)
    }
    val map = Texture2d(mipMapping = MipMapping.Off, name = "MsdfFont:$path")
        .apply {
            upload(img)
            KoolSystem.getContextOrNull()?.onShutdown += { release() }
        }
    return Result.success(MsdfFontData(map, meta))
}

@Suppress("ObjectPropertyName")
@PublishedApi
internal var _FONT_UI_DATA: MsdfFontData = MsdfFont.DEFAULT_FONT_DATA.also {
    Assets.launch {
        loadMsdfFontData("fonts/NotoSans").fold(
            {
                _FONT_UI_DATA = it
                UiUtils.triggerUpdateOnAllUiSurface()
            },
            { logW { "Failed to load UI font data: $it" } }
        )
    }
}

inline val MsdfFont.Companion.FONT_UI_DATA get() = _FONT_UI_DATA