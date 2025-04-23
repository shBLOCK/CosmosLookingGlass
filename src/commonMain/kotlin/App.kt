import de.fabmax.kool.KoolSystem
import de.fabmax.kool.math.MutableVec3d
import de.fabmax.kool.math.Vec2d
import de.fabmax.kool.math.Vec3d
import de.fabmax.kool.math.deg
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.util.Color
import de.fabmax.kool.util.DebugOverlay
import de.fabmax.kool.util.MsdfFont
import de.fabmax.kool.util.debugOverlay
import dynamics.SolarSystemKeplerModel3000BC3000AD
import dynamics.UniverseDynModelCollection
import dynamics.transformed
import ui.*
import universe.Universe
import universe.content.*
import utils.IntFract
import utils.projectSphere

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
                .transformed { time, position, orientation -> // to ICRF
                    position.rotate(23.43928.deg, Vec3d.X_AXIS)
                    orientation.rotate(23.43928.deg, Vec3d.X_AXIS)
                }
        )
    }
    val timeControl = TimeControl(IntFract(0))
    val time by timeControl::time

    val trailManager = TrailManager().apply {
        universe.root += mainTrailsRoot
        val sun = TrailManager.MainTrail(universe[Sun]!!, IntFract(SolarSystemConsts.EARTH_REVOLUTION), null)
            .also { this += it }
        val earth = TrailManager.MainTrail(universe[Earth]!!, IntFract(SolarSystemConsts.EARTH_REVOLUTION), sun)
            .also { this += it }
        this += TrailManager.MainTrail(universe[Moon]!!, IntFract(SolarSystemConsts.MOON_REVOLUTION), earth)
        this += TrailManager.MainTrail(universe[Mercury]!!, IntFract(SolarSystemConsts.MERCURY_REVOLUTION), sun)
        this += TrailManager.MainTrail(universe[Venus]!!, IntFract(SolarSystemConsts.VENUS_REVOLUTION), sun)
        this += TrailManager.MainTrail(universe[Mars]!!, IntFract(SolarSystemConsts.MARS_REVOLUTION), sun)
        this += TrailManager.MainTrail(universe[Jupiter]!!, IntFract(SolarSystemConsts.JUPITER_REVOLUTION), sun)
        this += TrailManager.MainTrail(universe[Saturn]!!, IntFract(SolarSystemConsts.SATURN_REVOLUTION), sun)
        this += TrailManager.MainTrail(universe[Uranus]!!, IntFract(SolarSystemConsts.URANUS_REVOLUTION), sun)
        this += TrailManager.MainTrail(universe[Neptune]!!, IntFract(SolarSystemConsts.NEPTUNE_REVOLUTION), sun)
    }

    val cameraControl = MainCameraControl(universe.scene.mainRenderPass.defaultView)
        .apply {
//            targetParams.halfSize = 24e7
            universe.scene.onUpdate {
//                //TODO: TEMP
//                val center = universe[Moon]!!.dynModel!!.position()
//                targetParams.center.set(center)
//                params.center.set(center)
            }
        }

    val hudSurface = UiSurface(name = "HudSurface").apply {
        content = {
            surface.sizes = UI_SIZES

            viewport.modifier.layout(FreeLayout)

            Button("Sun") {
                val sun = universe[Sun]!!


//                solarSystem.scene.camera.projectViewport(
//                    sun.toGlobalCoords(MutableVec3d()),
//                    solarSystem.scene.mainRenderPass.viewport,
//                    spos
//                )
                var sph = universe.scene.camera.projectSphere(sun.toGlobalCoords(MutableVec3d()), sun.outlineRadius)
                val r = sph.majorRadius * universe.scene.mainRenderPass.viewport.height / 2.0
                var spos = sph.center * (universe.scene.mainRenderPass.viewport.height / 2.0)
                spos = Vec2d(spos.x, -spos.y)
                spos += Vec2d(
                    universe.scene.mainRenderPass.viewport.width.toDouble(),
                    universe.scene.mainRenderPass.viewport.height.toDouble()
                ) / 2.0
                modifier
                    .free(spos, AlignmentX.Center, AlignmentY.Center)
                    .size(300.dp, 300.dp)
                    .background(UiRenderer {
                        with(it) {
                            getUiPrimitives(0).localCircleBorder(150.dp.px, 150.dp.px, r.toFloat(), 3F, Color.GREEN)
                        }
                    })
                    .isBlocking(false)
            }
        }
    }

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
        timeControl.update()
        universe.time = time

        cameraControl.update()

        universe.update()

        trailManager.update(time)

        hudSurface.triggerUpdate() // always recompose HUD
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