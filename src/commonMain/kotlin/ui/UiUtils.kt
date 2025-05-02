@file:Suppress("NOTHING_TO_INLINE")

package ui

import de.fabmax.kool.Assets
import de.fabmax.kool.KoolSystem
import de.fabmax.kool.loadBlob
import de.fabmax.kool.loadImage2d
import de.fabmax.kool.math.Vec2d
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.pipeline.MipMapping
import de.fabmax.kool.pipeline.Texture2d
import de.fabmax.kool.util.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import platform.platformImg
import utils.FwdInvFunction

object UiUtils {
    fun triggerUpdateOnAllUiSurface() {
        KoolSystem.getContextOrNull()?.scenes?.forEach { scene ->
            scene.children.forEach { (it as? UiSurface)?.triggerUpdate() }
        }
    }
}

inline val Float.dpx get() = Dp.fromPx(this)
inline val Double.dpx get() = Dp.fromPx(this.toFloat())
inline val Int.dpx get() = Dp.fromPx(this.toFloat())

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

val AlignmentX.mirror get() = when (this) {
    AlignmentX.Start -> AlignmentX.End
    AlignmentX.Center -> AlignmentX.Center
    AlignmentX.End -> AlignmentX.Start
}

val AlignmentY.mirror get() = when (this) {
    AlignmentY.Top -> AlignmentY.Bottom
    AlignmentY.Center -> AlignmentY.Center
    AlignmentY.Bottom -> AlignmentY.Top
}

enum class AlignmentXY(val x: AlignmentX, val y: AlignmentY, val anchor: Vec2d) {
    //@formatter:off
    TopLeft    (AlignmentX.Start,  AlignmentY.Top,    Vec2d(0.0, 0.0)),
    Top        (AlignmentX.Center, AlignmentY.Top,    Vec2d(0.5, 0.0)),
    TopRight   (AlignmentX.End,    AlignmentY.Top,    Vec2d(1.0, 0.0)),
    Left       (AlignmentX.Start,  AlignmentY.Center, Vec2d(0.0, 0.5)),
    Center     (AlignmentX.Center, AlignmentY.Center, Vec2d(0.5, 0.5)),
    Right      (AlignmentX.End,    AlignmentY.Center, Vec2d(1.0, 0.5)),
    BottomLeft (AlignmentX.Start,  AlignmentY.Bottom, Vec2d(0.0, 1.0)),
    Bottom     (AlignmentX.Center, AlignmentY.Bottom, Vec2d(0.5, 1.0)),
    BottomRight(AlignmentX.End,    AlignmentY.Bottom, Vec2d(1.0, 1.0));
    //@formatter:on

    val mirror by lazy { AlignmentXY(x.mirror, y.mirror) }
    
    companion object {
        operator fun invoke(x: AlignmentX, y: AlignmentY) = when (y) {
            AlignmentY.Top -> when (x) {
                AlignmentX.Start -> TopLeft
                AlignmentX.Center -> Top
                AlignmentX.End -> TopRight
            }

            AlignmentY.Center -> when (x) {
                AlignmentX.Start -> Left
                AlignmentX.Center -> Center
                AlignmentX.End -> Right
            }

            AlignmentY.Bottom -> when (x) {
                AlignmentX.Start -> BottomLeft
                AlignmentX.Center -> Bottom
                AlignmentX.End -> BottomRight
            }
        }
    }
}

fun UiModifier.align(alignment: AlignmentXY) = align(alignment.x, alignment.y)

private suspend fun loadMsdfFontData(path: String): Result<MsdfFontData> {
    val img = Assets.loadImage2d(Assets.platformImg(path, lossless = true))
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