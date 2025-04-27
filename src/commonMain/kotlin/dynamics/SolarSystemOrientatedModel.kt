@file:Suppress("LocalVariableName")

package dynamics

import universe.CelestialBody
import universe.content.*
import kotlin.math.cos
import kotlin.math.sin

//TODO: more modular dyn model system (registering system; separate pos & orientation providers)
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

    // https://astropedia.astrogeology.usgs.gov/download/Docs/WGCCRE/WGCCRE2015reprint.pdf
    private val sunOri: DynModel.Orientation = IAUOrientationModel { d, T ->
        a0 = 286.13
        d0 = 63.87
        W = 84.176 + 14.1844000 * d
    }
    private val mercuryOri: DynModel.Orientation = IAUOrientationModel { d, T ->
        a0 = 281.0103 - 0.0328 * T
        d0 = 61.4155 - 0.0049 * T
        W = (329.5988 + 6.1385108 * d
            + 0.01067257 * sin(174.7910857 + 4.092335 * d)
            - 0.00112309 * sin(349.5821714 + 8.184670 * d)
            - 0.00011040 * sin(164.3732571 + 12.277005 * d)
            - 0.00002539 * sin(339.1643429 + 16.369340 * d)
            - 0.00000571 * sin(153.9554286 + 20.461675 * d))
    }
    private val venusOri: DynModel.Orientation = IAUOrientationModel { d, T ->
        a0 = 272.76
        d0 = 67.16
        W = 160.20 - 1.4813688 * d
    }
    private val earthOri: DynModel.Orientation = IAUOrientationModel { d, T ->
        // https://aa.usno.navy.mil/downloads/reports/Archinaletal2011a.pdf
        a0 = 0.00 - 0.641 * T
        d0 = 90.00 - 0.557 * T
        W = 190.147 + 360.9856235 * d
    }
    private val moonOri: DynModel.Orientation = IAUOrientationModel { d, T ->
        val E1 = 125.045 - 0.0529921 * d
        val E2 = 250.089 - 0.1059842 * d
        val E3 = 260.008 + 13.0120009 * d
        val E4 = 176.625 + 13.3407154 * d
        val E5 = 357.529 + 0.9856003 * d
        val E6 = 311.589 + 26.4057084 * d
        val E7 = 134.963 + 13.0649930 * d
        val E8 = 276.617 + 0.3287146 * d
        val E9 = 34.226 + 1.7484877 * d
        val E10 = 15.134 - 0.1589763 * d
        val E11 = 119.743 + 0.0036096 * d
        val E12 = 239.961 + 0.1643573 * d
        val E13 = 25.053 + 12.9590088 * d
        a0 = (269.9949 + 0.0031 * T - 3.8787 * sin(E1) - 0.1204 * sin(E2)
            + 0.0700 * sin(E3) - 0.0172 * sin(E4) + 0.0072 * sin(E6)
            - 0.0052 * sin(E10) + 0.0043 * sin(E13))
        d0 = (66.5392 + 0.0130 * T + 1.5419 * cos(E1) + 0.0239 * cos(E2)
            - 0.0278 * cos(E3) + 0.0068 * cos(E4) - 0.0029 * cos(E6)
            + 0.0009 * cos(E7) + 0.0008 * cos(E10) - 0.0009 * cos(E13))
        W = (38.3213 + 13.17635815 * d - 1.4e-12 * d * d + 3.5610 * sin(E1)
            + 0.1208 * sin(E2) - 0.0642 * sin(E3) + 0.0158 * sin(E4)
            + 0.0252 * sin(E5) - 0.0066 * sin(E6) - 0.0047 * sin(E7)
            - 0.0046 * sin(E8) + 0.0028 * sin(E9) + 0.0052 * sin(E10)
            + 0.0040 * sin(E11) + 0.0019 * sin(E12) - 0.0044 * sin(E13))
    }
    private val marsOri: DynModel.Orientation = IAUOrientationModel { d, T ->
        a0 = 317.68143 - 0.1061 * T
        d0 = 52.88650 - 0.0609 * T
        W = 176.630 + 350.89198226 * d
    }
    private val jupiterOri: DynModel.Orientation = IAUOrientationModel { d, T ->
        val Ja = 99.360714 + 4850.4046 * T
        val Jb = 175.895369 + 1191.9605 * T
        val Jc = 300.323162 + 262.5475 * T
        val Jd = 114.012305 + 6070.2476 * T
        val Je = 49.511251 + 64.3000 * T
        a0 = (268.056595 - 0.006499 * T + 0.000117 * sin(Ja) + 0.000938 * sin(Jb)
            + 0.001432 * sin(Jc) + 0.000030 * sin(Jd) + 0.002150 * sin(Je))
        d0 = (64.495303 + 0.002413 * T + 0.000050 * cos(Ja) + 0.000404 * cos(Jb)
            + 0.000617 * cos(Jc) - 0.000013 * cos(Jd) + 0.000926 * cos(Je))
        W = 284.95 + 870.5360000 * d
    }
    private val saturnOri: DynModel.Orientation = IAUOrientationModel { d, T ->
        a0 = 40.589 - 0.036 * T
        d0 = 83.537 - 0.004 * T
        W = 38.90 + 810.7939024 * d
    }
    private val uranusOri: DynModel.Orientation = IAUOrientationModel { d, T ->
        a0 = 257.311
        d0 = -15.175
        W = 203.81 - 501.1600928 * d
    }
    private val neptuneOri: DynModel.Orientation = IAUOrientationModel { d, T ->
        val N = 357.85 + 52.316 * T
        a0 = 299.36 + 0.70 * sin(N)
        d0 = 43.46 - 0.51 * cos(N)
        W = 253.18 + 536.3128492 * d - 0.48 * sin(N)
    }

    //@formatter:off
    private val sun     by lazy { CelestialDynModel.Composed(sunPos,     sunOri)     }
    private val mercury by lazy { CelestialDynModel.Composed(mercuryPos, mercuryOri) }
    private val venus   by lazy { CelestialDynModel.Composed(venusPos,   venusOri)   }
    private val earth   by lazy { CelestialDynModel.Composed(earthPos,   earthOri)   }
    private val moon    by lazy { CelestialDynModel.Composed(moonPos,    moonOri)    }
    private val mars    by lazy { CelestialDynModel.Composed(marsPos,    marsOri)    }
    private val jupiter by lazy { CelestialDynModel.Composed(jupiterPos, jupiterOri) }
    private val saturn  by lazy { CelestialDynModel.Composed(saturnPos,  saturnOri)  }
    private val uranus  by lazy { CelestialDynModel.Composed(uranusPos,  uranusOri)  }
    private val neptune by lazy { CelestialDynModel.Composed(neptunePos, neptuneOri) }
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

