import de.fabmax.kool.Assets
import de.fabmax.kool.KoolSystem
import de.fabmax.kool.modules.ksl.blocks.ColorSpaceConversion
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.scene.Skybox
import de.fabmax.kool.util.DebugOverlay
import de.fabmax.kool.util.MsdfFont
import de.fabmax.kool.util.debugOverlay
import dynamics.SolarSystemKeplerModel3000BC3000AD
import dynamics.UniverseDynModelCollection
import kotlinx.coroutines.launch
import platform.platformImg
import ui.FONT_UI_DATA
import ui.MainCameraControl
import ui.TimeControl
import ui.TrailManager
import ui.hud.HudViewport
import ui.hud.UniverseCelestialBodyHudButtons
import universe.SingletonCelestialBody
import universe.Universe
import universe.content.*
import utils.IntFract
import utils.loadTextureCube

class App() {
    val koolCtx = KoolSystem.requireContext()

    val universe = Universe().apply {
        this += Sun()
        this += Mercury()
        this += Venus()
        this += Earth()
        this += Moon()
        this += Mars()
        this += Jupiter()
        this += Saturn()
        this += Uranus()
        this += Neptune()
        dynModel = UniverseDynModelCollection(
            SolarSystemKeplerModel3000BC3000AD()
        )

        Assets.launch {
            Assets.defaultLoader.loadTextureCube(Assets.platformImg("cdn/textures/misc/background_starmap"))
                .onSuccess {
                    root += Skybox.cube(it, 1F, ColorSpaceConversion.AsIs, scene.depthMode)
                }
        }

//        root.addDebugAxisIndicator(1e11)
    }

    val cameraControl = MainCameraControl(universe.scene.mainRenderPass.defaultView)
        .apply {
            targetParams.halfSize = 24e7
//            targetParams.halfSize = 10e5
            universe.scene.onUpdate {
//                //TODO: TEMP
//                val center = universe[Earth]!!.dynModel!!.position()
//                targetParams.center.set(center)
//                params.center.set(center)
            }
        }

    val trailManager = TrailManager().apply {
        universe.root += mainTrailsRoot
        val sun = TrailManager.MainTrail(universe[Sun]!!, IntFract(SolarSystemConsts.EARTH_REVOLUTION), null)
            .also { this += it }
        val earth = TrailManager.MainTrail(universe[Earth]!!, IntFract(SolarSystemConsts.EARTH_REVOLUTION), sun)
            .apply { stepSize = 3600 * 4 }
            .also { this += it }
        this += TrailManager.MainTrail(universe[Moon]!!, IntFract(SolarSystemConsts.MOON_REVOLUTION), earth)
            .apply { stepSize = 3600 * 4 }
        this += TrailManager.MainTrail(universe[Mercury]!!, IntFract(SolarSystemConsts.MERCURY_REVOLUTION), sun)
            .apply { stepSize = 3600 * 8 }
        this += TrailManager.MainTrail(universe[Venus]!!, IntFract(SolarSystemConsts.VENUS_REVOLUTION), sun)
        this += TrailManager.MainTrail(universe[Mars]!!, IntFract(SolarSystemConsts.MARS_REVOLUTION), sun)
        this += TrailManager.MainTrail(universe[Jupiter]!!, IntFract(SolarSystemConsts.JUPITER_REVOLUTION), sun)
        this += TrailManager.MainTrail(universe[Saturn]!!, IntFract(SolarSystemConsts.SATURN_REVOLUTION), sun)
        this += TrailManager.MainTrail(universe[Uranus]!!, IntFract(SolarSystemConsts.URANUS_REVOLUTION), sun)
        this += TrailManager.MainTrail(universe[Neptune]!!, IntFract(SolarSystemConsts.NEPTUNE_REVOLUTION), sun)
    }

    // region UI
    val hudUiSurface = UiSurface(name = "HudUiSurface").apply {
        content = {
            surface.sizes = UI_SIZES

            Column {
                modifier
                    .alignY(AlignmentY.Bottom)
                    .width(Grow.Std)

                timeControl()
            }
        }
    }

    val timeControl = TimeControl(IntFract(0))
    val time by timeControl::time
    // endregion

    // region HUD
    val hudSurface = UiSurface(name = "HudSurface").apply {
        content = {
            surface.sizes = UI_SIZES

            HudViewport {
                cameraControl.attachTo(this)
                celestialBodyHudButtons()
            }
        }
    }

    val celestialBodyHudButtons = UniverseCelestialBodyHudButtons(universe).apply {
        configurator = {
            if (it is SingletonCelestialBody<*, *>) {
                modifier.priority = when (it.companion) {
                    Sun -> 2
                    Earth -> 1
                    else -> 0
                }
            }
        }
    }
    // endregion

    val uiScene = UiScene {
        this += hudSurface
        this += hudUiSurface
    }

    /**
     * This happens before the Kool's [de.fabmax.kool.scene.Node.update].
     *
     * We do most of our app logic here for a predictable update order.
     */
    fun update() {
        universe.resetGlobalTransformHackForFloatPrecision()

        timeControl.update()
        universe.time = time

        cameraControl.update()

        universe.update()

        trailManager.update(time)

        hudSurface.triggerUpdate() // always recompose HUD

        universe.setGlobalTransformHackForFloatPrecision()
    }

    internal fun launch() {
        koolCtx.onRender += { update() }

        koolCtx.scenes += universe.scene
        koolCtx.scenes += uiScene

        koolCtx.scenes += debugOverlay(DebugOverlay.Position.LOWER_RIGHT)
    }
}

private val UI_SIZES = Sizes.medium(
    normalText = MsdfFont(MsdfFont.FONT_UI_DATA, sizePts = 15F),
    largeText = MsdfFont(MsdfFont.FONT_UI_DATA, sizePts = 20F)
)