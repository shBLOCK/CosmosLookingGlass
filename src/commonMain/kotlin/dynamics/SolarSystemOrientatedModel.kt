package dynamics

import de.fabmax.kool.math.QuatD

abstract class SolarSystemOrientatedModel : SolarSystemDynModel() {
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
    override val sun     by lazy { CelestialDynModel.Composed(sunPos,     DynModel.Orientation.Static(QuatD.IDENTITY)) }
    override val mercury by lazy { CelestialDynModel.Composed(mercuryPos, DynModel.Orientation.Static(QuatD.IDENTITY)) }
    override val venus   by lazy { CelestialDynModel.Composed(venusPos,   DynModel.Orientation.Static(QuatD.IDENTITY)) }
    override val earth   by lazy { CelestialDynModel.Composed(earthPos,   DynModel.Orientation.Static(QuatD.IDENTITY)) }
    override val moon    by lazy { CelestialDynModel.Composed(moonPos,    DynModel.Orientation.Static(QuatD.IDENTITY)) }
    override val mars    by lazy { CelestialDynModel.Composed(marsPos,    DynModel.Orientation.Static(QuatD.IDENTITY)) }
    override val jupiter by lazy { CelestialDynModel.Composed(jupiterPos, DynModel.Orientation.Static(QuatD.IDENTITY)) }
    override val saturn  by lazy { CelestialDynModel.Composed(saturnPos,  DynModel.Orientation.Static(QuatD.IDENTITY)) }
    override val uranus  by lazy { CelestialDynModel.Composed(uranusPos,  DynModel.Orientation.Static(QuatD.IDENTITY)) }
    override val neptune by lazy { CelestialDynModel.Composed(neptunePos, DynModel.Orientation.Static(QuatD.IDENTITY)) }
    //@formatter:on
}

