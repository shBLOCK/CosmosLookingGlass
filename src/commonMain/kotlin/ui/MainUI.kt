package ui

import de.fabmax.kool.KoolContext
import de.fabmax.kool.modules.ui2.AlignmentX
import de.fabmax.kool.modules.ui2.AlignmentY
import de.fabmax.kool.modules.ui2.RectBackground
import de.fabmax.kool.modules.ui2.Sizes
import de.fabmax.kool.modules.ui2.UiNode
import de.fabmax.kool.modules.ui2.UiScene
import de.fabmax.kool.modules.ui2.UiSurface
import de.fabmax.kool.modules.ui2.align
import de.fabmax.kool.modules.ui2.background
import de.fabmax.kool.modules.ui2.layout
import de.fabmax.kool.modules.ui2.size
import de.fabmax.kool.util.BaseReleasable
import de.fabmax.kool.util.Color
import de.fabmax.kool.util.MdColor
import de.fabmax.kool.util.MsdfFont
import de.fabmax.kool.util.Time
import universe.CelestialBody
import universe.SolarSystemScene
import utils.FwdInvFunction
import utils.IntFract
import utils.max
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.withSign

class MainUI(private val solarSystem: SolarSystemScene) : BaseReleasable() {
    private var time by solarSystem::time

    val cameraControl = MainCameraControl(solarSystem.mainRenderPass.defaultView)
        .apply { solarSystem.onUpdate { update() } }

    private lateinit var timeControl: TimeControl

    val cbToTrail = mutableMapOf<CelestialBody, Trail>()

    private val trailManager = TrailManager().apply {
        solarSystem.onUpdate { update() }
        solarSystem.celestialBodies.forEach { body ->
            trails += Trail(body).also {
                solarSystem += it.meshInstances[0]
            }.also { cbToTrail[body] = it }
        }
    }

    init {
        trailManager.trails.forEach {
            it.meshInstances[0].ref = cbToTrail[solarSystem.sun]
            it.meshInstances[0].alterRef = cbToTrail[solarSystem.earth]
        }
    }

    var test = 0

    private fun getSizes() = Sizes.medium(
        normalText = MsdfFont(MsdfFont.FONT_UI_DATA, sizePts = 15F),
        largeText = MsdfFont(MsdfFont.FONT_UI_DATA, sizePts = 20F)
    )

    val hudSurface = UiSurface(name = "HudSurface").apply {
        onUpdate { triggerUpdate() }
        content = {
            surface.sizes = getSizes()

            viewport.modifier.layout(FreeLayout)

//            for (pl in listOf<CelestialBody>(solarSystem.earth, solarSystem.mercury, solarSystem.jupiter).shuffled()) {
//                val pos = pl.toGlobalCoords(MutableVec3d())
//                if (solarSystem.camera.dataD.globalLookDir dot (pos - solarSystem.camera.dataD.globalPos) <= 0.0) continue
//                Button("${pl.name} Test ${test++}", scopeName = pl.name) {
//                    val spos = MutableVec3d()
//                        .also {
//                            solarSystem.camera.projectViewport(
//                                pos,
//                                solarSystem.mainRenderPass.viewport,
//                                it
//                            )
//                        }
//                    modifier.free(spos.xy, AlignmentX.Center, AlignmentY.Center)
//                }
//            }
        }
    }

    val hudUiSurface = UiSurface(name = "HudUiSurface").apply {
        content = {
            surface.sizes = getSizes()

                timeControl = uiNode.createChild("TimeControl", TimeControl::class, ::TimeControl).apply {
                    setup()
                    modifier
                        .alignX(AlignmentX.Center)
                        .size(1000.dp, 50.dp)
                        .background(RectBackground(MdColor.GREY.withAlpha(0.3F)))
                }
            }
        }
    }

    val scene = UiScene {
        this += hudSurface
        this += hudUiSurface
    }

    private inner class TrailManager {
        val trails = mutableListOf<Trail>()

        fun update() {
            trails.forEach {
                it.currentTime = solarSystem.time
                it.endTime = solarSystem.time
                it.startTime = max(IntFract.ZERO, solarSystem.time - 100 * 365.25 * 24 * 3600)
//                it.startTime = it.endTime - IntFract(3600L * 24 * 365 * 100)
//                it.startTime = it.endTime - IntFract(3600L * 24 * 365)
//                it.meshInstances[0].refMix = sin(Time.gameTime) * .5 + .5
            }
            trails.forEach { it.update() }
        }
    }

    private inner class TimeControl(parent: UiNode?, surface: UiSurface) : SlottedReboundSliderNode(parent, surface) {
        init {
            solarSystem.onUpdate {
                time += value.value * Time.deltaT
            }

            val TO_ONE = 0.2
            posToValue = FwdInvFunction(
                forward = {
                    val x = abs(it)
                    (if (x > TO_ONE) (10.0.pow(x - TO_ONE)) else (x * (1.0 / TO_ONE))).withSign(it)
                },
                inverse = {
                    val x = abs(it)
                    (if (x > 1.0) (log10(x) + TO_ONE) else (x * TO_ONE)).withSign(it)
                }
            )

            val MIN = 60.0
            val HOUR = 3600.0
            val DAY = 24.0 * HOUR
            val MONTH = 30.0 * DAY
            val YEAR = 365.25 * DAY
            val DECADE = 10.0 * YEAR
            val CENTURY = 10.0 * DECADE
            slots = listOf(
                1.0, 10.0, 30.0,
                MIN, 10 * MIN, 30 * MIN,
                HOUR, 3 * HOUR, 10 * HOUR,
                DAY, 3 * DAY, 10 * DAY,
                MONTH, 6 * MONTH,
                YEAR, 3 * YEAR,
                DECADE,
                CENTURY
            ).let { it.map { -it }.reversed() + it }
        }

        override fun render(ctx: KoolContext) {
            super.render(ctx)

            val railY = slotInsertionToPx(0.0).toFloat()
            val posCenterPx = posToPx(0.0).toFloat()
            getPlainBuilder(0).configured {
                rect {
                    isCenteredOrigin = false
                    origin.x = posCenterPx
                    origin.y = railY - lineWidth.px / 2F
                    size.x = (posToPx(pos.value) - posCenterPx).toFloat()
                    size.y = lineWidth.px
                    color = Color.GREEN
                }
            }
        }
    }
}