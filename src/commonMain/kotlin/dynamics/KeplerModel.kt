package dynamics

import de.fabmax.kool.math.MutableVec2d
import de.fabmax.kool.math.Vec3d
import de.fabmax.kool.math.deg
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.reflect.KProperty

/**
 * An approximating dynamics model using Keplerian formulae. See [this NASA JPL Document](https://ssd.jpl.nasa.gov/planets/approx_pos.html).
 *
 * Uses the [ICRF](https://en.wikipedia.org/wiki/International_Celestial_Reference_System_and_its_realizations) reference frame.
 */
@Suppress("LocalVariableName", "PrivatePropertyName")
class KeplerModel(
    a: Double,
    e: Double,
    I: Double,
    L: Double,
    lp: Double,
    lan: Double,

    da: Double,
    de: Double,
    dI: Double,
    dL: Double,
    dlp: Double,
    dlan: Double,

    // fine-tune values
    private val ftB: Double = 0.0,
    private val ftC: Double = 0.0,
    private val ftS: Double = 0.0,
    private val ftF: Double = 0.0
) : DynModelBase(), DynModel.Position {

    inner class DriftingParam(
        val origin: Double,
        val drift: Double
    ) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>) =
            origin + drift * time.centuries
    }

    /** semi-major axis */
    private val a by DriftingParam(a, da)

    /** eccentricity */
    private val e by DriftingParam(e, de)

    /** inclination */
    private val I by DriftingParam(I, dI)

    /** mean longitude */
    private val L by DriftingParam(L, dL)

    /** longitude of perihelion */
    private val lp by DriftingParam(lp, dlp)

    /** longitude of the ascending node */
    private val lan by DriftingParam(lan, dlan)


    override fun position(): Vec3d {
        val T = time.centuries
        val ap = lp - lan
        val M_full = L - lp + ftB * T * T + ftC * cos(ftF * T) + ftS * sin(ftF * T)
        val M = (M_full + PI).mod(PI * 2.0) - PI // to -pi..pi

        val E = solveKeplerEq(e, M)

        val hx = a * (cos(E) - e)
        val hy = a * sqrt(1.0 - e * e) * sin(E)

        val xEcl = (cos(ap) * cos(lan) - sin(ap) * sin(lan) * cos(I)) * hx +
            (-sin(ap) * cos(lan) - cos(ap) * sin(lan) * cos(I)) * hy
        val yEcl = (cos(ap) * sin(lan) + sin(ap) * cos(lan) * cos(I)) * hx +
            (-sin(ap) * sin(lan) + cos(ap) * cos(lan) * cos(I)) * hy
        val zEcl = (sin(ap) * sin(I)) * hx +
            (cos(ap) * sin(I)) * hy

        return Vec3d(xEcl, MutableVec2d(yEcl, zEcl).rotate(23.43928.deg))
    }


    companion object {
        private const val KEPLER_MAX_ITER = 100
        private const val KEPLER_TOLERANCE = 1e-9

        /**
         * Solve *M = E - e * sin(E)* for E
         *
         * M (rad): mean anomaly,
         * E (rad): eccentric anomaly
         */
        private fun solveKeplerEq(pe: Double, m: Double): Double {
            var e = m + pe * sin(m)
            repeat(KEPLER_MAX_ITER) {
                val dm = m - (e - pe * sin(e))
                val de = dm / (1.0 - pe * cos(e))
                e += de
                if (abs(de) < KEPLER_TOLERANCE) return e
            }
            throw RuntimeException("Kepler equation failed to converge (M = $m, final E after $KEPLER_MAX_ITER iterations = $e)")
        }
    }
}
