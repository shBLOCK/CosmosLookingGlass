@file:Suppress("WrapUnaryOperator", "GrazieInspection")

package dynamics

import de.fabmax.kool.math.Vec3d
import de.fabmax.kool.math.deg
import universe.content.SolarSystemConsts
import utils.IntFractTime
import utils.SPEED_OF_LIGHT
import utils.au
import utils.er

abstract class SolarSystemKeplerModelBase : SolarSystemOrientatedModel() {
    //    override val sunPos = DynModel.Position.Static(Vec3d.ZERO)
    override val sunPos by lazy {
        BarycenterPositionModel(
            listOf(
                mercuryPos to SolarSystemConsts.MERCURY_MASS,
                venusPos to SolarSystemConsts.VENUS_MASS,
                earthPos to SolarSystemConsts.EARTH_MASS,
                moonPos to SolarSystemConsts.MOON_MASS,
                marsPos to SolarSystemConsts.MARS_MASS,
                jupiterPos to SolarSystemConsts.JUPITER_MASS,
                saturnPos to SolarSystemConsts.SATURN_MASS,
                uranusPos to SolarSystemConsts.URANUS_MASS,
                neptunePos to SolarSystemConsts.NEPTUNE_MASS
            ).filter { (model, objMass) -> objMass > 1e24 }, // ignore small mass objects
            mass = SolarSystemConsts.SUN_MASS,
            barycenter = Vec3d.ZERO,
//            gravityPropagationSpeed = SPEED_OF_LIGHT
            gravityPropagationSpeed = 0.0
        )
    }
}

private const val BC3000 = -50L * IntFractTime.J2000.CENTURY
private const val AD3000 = 10L * IntFractTime.J2000.CENTURY

class SolarSystemKeplerModel3000BC3000AD : SolarSystemKeplerModelBase() {
    //@formatter:off
    // Tip: disable IDEA inlay hints for proper formatting
    // https://ssd.jpl.nasa.gov/planets/approx_pos.html
    //                                         a               e            I                       L                 long. peri            long. node
    override val mercuryPos = KeplerModel( 0.38709843.au,  0.20563661,  7.00559432.deg.rad,    252.25166724.deg.rad,  77.45771895.deg.rad,  48.33961819.deg.rad,
    +                                      0.00000000.au,  0.00002123, -0.00590158.deg.rad, 149472.67486623.deg.rad,   0.15940013.deg.rad,  -0.12214182.deg.rad,                                                             timeMin = BC3000, timeMax = AD3000)
    override val venusPos   = KeplerModel( 0.72332102.au,  0.00676399,  3.39777545.deg.rad,    181.97970850.deg.rad, 131.76755713.deg.rad,  76.67261496.deg.rad,
    +                                     -0.00000026.au, -0.00005107,  0.00043494.deg.rad,  58517.81560260.deg.rad,   0.05679648.deg.rad,  -0.27274174.deg.rad,                                                             timeMin = BC3000, timeMax = AD3000)
    private  val emBaryPos  = KeplerModel( 1.00000018.au,  0.01673163, -0.00054346.deg.rad,    100.46691572.deg.rad, 102.93005885.deg.rad,  -5.11260389.deg.rad,
    +                                     -0.00000003.au, -0.00003661, -0.01337178.deg.rad,  35999.37306329.deg.rad,   0.31795260.deg.rad,  -0.24123856.deg.rad,                                                             timeMin = BC3000, timeMax = AD3000)
    override val marsPos    = KeplerModel( 1.52371243.au,  0.09336511,  1.85181869.deg.rad,     -4.56813164.deg.rad, -23.91744784.deg.rad,  49.71320984.deg.rad,
    +                                      0.00000097.au,  0.00009149, -0.00724757.deg.rad,  19140.29934243.deg.rad,   0.45223625.deg.rad,  -0.26852431.deg.rad,                                                             timeMin = BC3000, timeMax = AD3000)
    override val jupiterPos = KeplerModel( 5.20248019.au,  0.04853590,  1.29861416.deg.rad,     34.33479152.deg.rad,  14.27495244.deg.rad, 100.29282654.deg.rad,//     b            c            s           f
    +                                     -0.00002864.au,  0.00018026, -0.00322699.deg.rad,   3034.90371757.deg.rad,   0.18199196.deg.rad,   0.13024619.deg.rad, -0.00012452,  0.06064060, -0.35635438, 38.35125000.deg.rad, timeMin = BC3000, timeMax = AD3000)
    override val saturnPos  = KeplerModel( 9.54149883.au,  0.05550825,  2.49424102.deg.rad,     50.07571329.deg.rad,  92.86136063.deg.rad, 113.63998702.deg.rad,
    +                                     -0.00003065.au, -0.00032044,  0.00451969.deg.rad,   1222.11494724.deg.rad,   0.54179478.deg.rad,  -0.25015002.deg.rad,  0.00025899, -0.13434469,  0.87320147, 38.35125000.deg.rad, timeMin = BC3000, timeMax = AD3000)
    override val uranusPos  = KeplerModel(19.18797948.au,  0.04685740,  0.77298127.deg.rad,    314.20276625.deg.rad, 172.43404441.deg.rad,  73.96250215.deg.rad,
    +                                     -0.00020455.au, -0.00001550, -0.00180155.deg.rad,    428.49512595.deg.rad,   0.09266985.deg.rad,   0.05739699.deg.rad,  0.00058331, -0.97731848,  0.17689245,  7.67025000.deg.rad, timeMin = BC3000, timeMax = AD3000)
    override val neptunePos = KeplerModel(30.06952752.au,  0.00895439,  1.77005520.deg.rad,    304.22289287.deg.rad,  46.68158724.deg.rad, 131.78635853.deg.rad,
    +                                      0.00006447.au,  0.00000818,  0.00022400.deg.rad,    218.46515314.deg.rad,   0.01009938.deg.rad,  -0.00606302.deg.rad, -0.00041348,  0.68346318, -0.10162547,  7.67025000.deg.rad, timeMin = BC3000, timeMax = AD3000)

    // https://stjarnhimlen.se/comp/ppcomp.html
    // TODO: perturbation terms (https://stjarnhimlen.se/comp/ppcomp.html#9)
    private  val _moonPos   = PPCompModel(125.1228      .deg.rad, 5.1454.deg.rad, 318.0634      .deg.rad, 60.2666.er, 0.054900, 115.3654      .deg.rad,
    +                                      -0.0529538083.deg.rad, 0.0   .deg.rad,   0.1643573223.deg.rad,  0.0   .er, 0.0     ,  13.0649929509.deg.rad)
    //@formatter:on

    override val moonPos = _moonPos.relativeTo(emBaryPos)
    override val earthPos = BarycenterPositionModel(
        listOf(_moonPos.copyTyped() to SolarSystemConsts.MOON_MASS),
        mass = SolarSystemConsts.EARTH_MASS,
        barycenter = Vec3d.ZERO,
        gravityPropagationSpeed = 0.0
    ).relativeTo(emBaryPos)

    //    override val moonPos: DynModel.Position<*> = object : DynModelBase<>(), DynModel.Position<*> { // TODO
//        override fun position() = earthPos.position() + Vec3d(384400e3, 0.0, 0.0)
//    }
}

/**
 * https://stjarnhimlen.se/comp/ppcomp.html
 */
@Suppress("LocalVariableName", "FunctionName")
private fun PPCompModel(
    N: Double, i: Double, w: Double, a: Double, e: Double, M: Double,
    dN: Double, di: Double, dw: Double, da: Double, de: Double, dM: Double
): KeplerModel {
    val T = IntFractTime.J2000.CENTURY / (24 * 3600)
    val lp = N + w
    val L = M + lp
    val dlp = dN + dw
    val dL = dM + dlp
    return KeplerModel(
        a, e, i, L, lp, N,
        da * T, de * T, di * T, dL * T, dlp * T, dN * T
    )
}