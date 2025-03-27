@file:Suppress("NOTHING_TO_INLINE")

package utils

import de.fabmax.kool.math.MutableQuatD
import de.fabmax.kool.math.MutableVec3d
import de.fabmax.kool.math.QuatD
import de.fabmax.kool.math.Vec3d
import de.fabmax.kool.math.clamp
import de.fabmax.kool.math.expDecay
import de.fabmax.kool.math.isFuzzyEqual
import de.fabmax.kool.util.Time
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin

const val SPEED_OF_LIGHT = 299792458.0

inline val Double.au get() = this * 149_597_870_700.0

fun Double.expDecaySnapping(target: Double, decay: Double, deltaT: Float = Time.deltaT) =
    if (!isFuzzyEqual(this, target)) expDecay(target, decay, deltaT) else target

fun MutableVec3d.expDecaySnapping(target: Vec3d, decay: Double, deltaT: Float = Time.deltaT): MutableVec3d {
    x = x.expDecaySnapping(target.x, decay, deltaT)
    y = y.expDecaySnapping(target.y, decay, deltaT)
    z = z.expDecaySnapping(target.z, decay, deltaT)
    return this
}

fun slerpShortest(quatA: QuatD, quatB: QuatD, f: Double, result: MutableQuatD = MutableQuatD()): MutableQuatD {
    // copied from kool TODO: verify if this actually does slerp shortest
    val qa = MutableQuatD()
    val qb = MutableQuatD()
    val qc = MutableQuatD()

    quatA.normed(qa)
    quatB.normed(qb)

    val t = f.clamp(0.0, 1.0)

    var dot = qa.dot(qb).clamp(-1.0, 1.0)
    if (dot < 0.0) {
        qa.mul(-1.0)
        dot = -dot
    }

    if (dot > (1.0 - 1e-10)) {
        qb.subtract(qa, result).mul(t).add(qa).norm()
    } else {
        val theta0 = acos(dot)
        val theta = theta0 * t

        qa.mul(-dot, qc).add(qb).norm()

        qa.mul(cos(theta))
        qc.mul(sin(theta))
        result.set(qa).add(qc)
    }
    return result
}

val Int.nextPowerOfTwo get() = if (this and (this - 1) == 0) this else 1 shl (32 - countLeadingZeroBits())
val Int.prevPowerOfTwo get() = if (this and (this - 1) == 0) this else 1 shl (32 - countLeadingZeroBits() - 1)

fun Int.ceilDiv(other: Int) = this.floorDiv(other) + if (this % other == 0) 0 else 1