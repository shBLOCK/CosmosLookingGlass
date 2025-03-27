package dynamics

import de.fabmax.kool.math.MutableVec2d
import de.fabmax.kool.math.MutableVec3d
import de.fabmax.kool.math.deg
import de.fabmax.kool.math.wrap
import utils.IntFract
import utils.j2000Centuries
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * An approximating dynamics model using Keplerian formulae. See [this NASA JPL Document](https://ssd.jpl.nasa.gov/planets/approx_pos.html).
 *
 * a: semi-major axis
 * e: eccentricity
 * I: inclination
 * L: mean longitude
 * lp: longitude of perihelion
 * lan: longitude of the ascending node
 *
 * Uses the [ICRF](https://en.wikipedia.org/wiki/International_Celestial_Reference_System_and_its_realizations) reference frame.
 */
@Suppress("LocalVariableName", "PrivatePropertyName")
class KeplerModel(
    private val _a: Double,
    private val _e: Double,
    private val _I: Double,
    private val _L: Double,
    private val _lp: Double,
    private val _lan: Double,

    private val _da: Double,
    private val _de: Double,
    private val _dI: Double,
    private val _dL: Double,
    private val _dlp: Double,
    private val _dlan: Double,

    // fine-tune values
    private val ftB: Double = 0.0,
    private val ftC: Double = 0.0,
    private val ftS: Double = 0.0,
    private val ftF: Double = 0.0,

    private val timeMin: Long = Long.MIN_VALUE,
    private val timeMax: Long = Long.MAX_VALUE
) : DynModelBase(), DynModel.Position {
    private var dirty = true
    override fun seek(time: IntFract) {
        super.seek(time)
        dirty = true
    }

    private val lastResult = MutableVec3d()

    private val tmpVec2d = MutableVec2d()

    override fun position(result: MutableVec3d): MutableVec3d {
        if (!dirty) return result.set(lastResult)

        val timeClamped = IntFract(time.int.coerceIn(timeMin, timeMax), time.fract)

        val centuriesClamped = timeClamped.j2000Centuries
        val ca = _a + _da * centuriesClamped
        val ce = _e + _de * centuriesClamped
        val cI = _I + _dI * centuriesClamped
        val cL = _L + _dL * centuriesClamped
        val clp = _lp + _dlp * centuriesClamped
        val clan = _lan + _dlan * centuriesClamped

        val ap = clp - clan
        val M_full = cL - clp + ftB * centuriesClamped * centuriesClamped +
            ftC * cos(ftF * centuriesClamped) + ftS * sin(ftF * centuriesClamped)
        val M = M_full.wrap(-PI, PI)

        val E = solveKeplerEq(ce, M)

        val hx = ca * (cos(E) - ce)
        val hy = ca * sqrt(1.0 - ce * ce) * sin(E)

        val sin_ap = sin(ap)
        val cos_ap = cos(ap)
        val sin_I = sin(cI)
        val cos_I = cos(cI)
        val sin_lan = sin(clan)
        val cos_lan = cos(clan)

        val xEcl = (cos_ap * cos_lan - sin_ap * sin_lan * cos_I) * hx +
            (-sin_ap * cos_lan - cos_ap * sin_lan * cos_I) * hy
        val yEcl = (cos_ap * sin_lan + sin_ap * cos_lan * cos_I) * hx +
            (-sin_ap * sin_lan + cos_ap * cos_lan * cos_I) * hy
        val zEcl = (sin_ap * sin_I) * hx +
            (cos_ap * sin_I) * hy

        tmpVec2d.set(yEcl, zEcl).rotate(23.43928.deg)
        return result.set(xEcl, tmpVec2d.x, tmpVec2d.y).also { lastResult.set(it) }
    }

    companion object {
        private const val KEPLER_MAX_ITER = 100
        private const val KEPLER_TOLERANCE = 1e-6

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

    override fun copy() = KeplerModel(
        _a, _e, _I, _L, _lp, _lan, _da, _de, _dI, _dL, _dlp, _dlan,
        ftB, ftC, ftS, ftF,
        timeMin, timeMax
    )
}
