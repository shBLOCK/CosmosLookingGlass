@file:Suppress("WrapUnaryOperator")

package dynamics

import de.fabmax.kool.math.QuatD
import de.fabmax.kool.math.Vec3d
import utils.au
import utils.deg

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

class SolarSystemKeplerModel : SolarSystemOrientatedModel() {
    override val sunPos = DynModel.Position.Static(Vec3d.ZERO) // TODO: derive from `center of mass = barycenter`

    //@formatter:off
    //                                         a               e            I                   L             long. peri        long. node
    override val mercuryPos = KeplerModel( 0.38709843.au,  0.20563661,  7.00559432.deg,    252.25166724.deg,  77.45771895.deg,  48.33961819.deg,
    +                                      0.00000000.au,  0.00002123, -0.00590158.deg, 149472.67486623.deg,   0.15940013.deg,  -0.12214182.deg)
    override val venusPos   = KeplerModel( 0.72332102.au,  0.00676399,  3.39777545.deg,    181.97970850.deg, 131.76755713.deg,  76.67261496.deg,
    +                                     -0.00000026.au, -0.00005107,  0.00043494.deg,  58517.81560260.deg,   0.05679648.deg,  -0.27274174.deg)
    private  val emBaryPos  = KeplerModel( 1.00000018.au,  0.01673163, -0.00054346.deg,    100.46691572.deg, 102.93005885.deg,  -5.11260389.deg,
    +                                     -0.00000003.au, -0.00003661, -0.01337178.deg,  35999.37306329.deg,   0.31795260.deg,  -0.24123856.deg)
    override val marsPos    = KeplerModel( 1.52371243.au,  0.09336511,  1.85181869.deg,     -4.56813164.deg, -23.91744784.deg,  49.71320984.deg,
    +                                      0.00000097.au,  0.00009149, -0.00724757.deg,  19140.29934243.deg,   0.45223625.deg,  -0.26852431.deg)
    override val jupiterPos = KeplerModel( 5.20248019.au,  0.04853590,  1.29861416.deg,     34.33479152.deg,  14.27495244.deg, 100.29282654.deg,//     b            c            s           f
    +                                     -0.00002864.au,  0.00018026, -0.00322699.deg,   3034.90371757.deg,   0.18199196.deg,   0.13024619.deg, -0.00012452,  0.06064060, -0.35635438, 38.35125000)
    override val saturnPos  = KeplerModel( 9.54149883.au,  0.05550825,  2.49424102.deg,     50.07571329.deg,  92.86136063.deg, 113.63998702.deg,
    +                                     -0.00003065.au, -0.00032044,  0.00451969.deg,   1222.11494724.deg,   0.54179478.deg,  -0.25015002.deg,  0.00025899, -0.13434469,  0.87320147, 38.35125000)
    override val uranusPos  = KeplerModel(19.18797948.au,  0.04685740,  0.77298127.deg,    314.20276625.deg, 172.43404441.deg,  73.96250215.deg,
    +                                     -0.00020455.au, -0.00001550, -0.00180155.deg,    428.49512595.deg,   0.09266985.deg,   0.05739699.deg,  0.00058331, -0.97731848,  0.17689245,  7.67025000)
    override val neptunePos = KeplerModel(30.06952752.au,  0.00895439,  1.77005520.deg,    304.22289287.deg,  46.68158724.deg, 131.78635853.deg,
    +                                      0.00006447.au,  0.00000818,  0.00022400.deg,    218.46515314.deg,   0.01009938.deg,  -0.00606302.deg, -0.00041348,  0.68346318, -0.10162547,  7.67025000)
    //@formatter:on

    override val earthPos = emBaryPos // TODO
    override val moonPos: DynModel.Position = object : DynModelBase(), DynModel.Position { // TODO
        override fun position() = earthPos.position() + Vec3d(384400e3, 0.0, 0.0)
    }
}