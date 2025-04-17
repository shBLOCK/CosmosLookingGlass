package dynamics

import de.fabmax.kool.math.QuatD
import universe.CelestialBody
import universe.content.*

abstract class SolarSystemOrientatedModel : UniverseDynModelImpl() {
    protected abstract val sunPos: DynModel.Position
    protected abstract val mercuryPos: DynModel.Position
    protected abstract val venusPos: DynModel.Position
    protected abstract val earthPos: DynModel.Position
    protected abstract val moonPos: DynModel.Position
    protected abstract val marsPos: DynModel.Position
    protected abstract val jupiterPos: DynModel.Position
    protected abstract val saturnPos: DynModel.Position
    protected abstract val uranusPos: DynModel.Position
    protected abstract val neptunePos: DynModel.Position

    //TODO: actual orientation model
    //@formatter:off
    private val sun     by lazy { CelestialDynModel.Composed(sunPos,     DynModel.Orientation.Static(QuatD.IDENTITY)) }
    private val mercury by lazy { CelestialDynModel.Composed(mercuryPos, DynModel.Orientation.Static(QuatD.IDENTITY)) }
    private val venus   by lazy { CelestialDynModel.Composed(venusPos,   DynModel.Orientation.Static(QuatD.IDENTITY)) }
    private val earth   by lazy { CelestialDynModel.Composed(earthPos,   DynModel.Orientation.Static(QuatD.IDENTITY)) }
    private val moon    by lazy { CelestialDynModel.Composed(moonPos,    DynModel.Orientation.Static(QuatD.IDENTITY)) }
    private val mars    by lazy { CelestialDynModel.Composed(marsPos,    DynModel.Orientation.Static(QuatD.IDENTITY)) }
    private val jupiter by lazy { CelestialDynModel.Composed(jupiterPos, DynModel.Orientation.Static(QuatD.IDENTITY)) }
    private val saturn  by lazy { CelestialDynModel.Composed(saturnPos,  DynModel.Orientation.Static(QuatD.IDENTITY)) }
    private val uranus  by lazy { CelestialDynModel.Composed(uranusPos,  DynModel.Orientation.Static(QuatD.IDENTITY)) }
    private val neptune by lazy { CelestialDynModel.Composed(neptunePos, DynModel.Orientation.Static(QuatD.IDENTITY)) }
    //@formatter:on

    override fun getDynModelFor(celestialBody: CelestialBody) =
        when (celestialBody) {
            is Sun -> sun
            is Mercury -> mercury
            is Venus -> venus
            is Earth -> earth
            is Moon -> moon
            is Mars -> mars
            is Jupiter -> jupiter
            is Saturn -> saturn
            is Uranus -> uranus
            is Neptune -> neptune
            else -> super.getDynModelFor(celestialBody)
        }
}

