@file:Suppress("NOTHING_TO_INLINE")

package utils

import de.fabmax.kool.math.*
import de.fabmax.kool.scene.Camera
import de.fabmax.kool.scene.OrthographicCamera
import de.fabmax.kool.scene.PerspectiveCamera
import de.fabmax.kool.util.Time
import kotlin.math.*

const val SPEED_OF_LIGHT = 299792458.0

inline val Double.au get() = this * 149_597_870_700.0

/** Earth radii */
inline val Double.er get() = this * 6.3781366e6

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

inline fun atan2(xy: Vec2d) = atan2(xy.y, xy.x)

fun MutableVec3d.rotate(angle: AngleD, axis: RayD) = this.apply {
    subtract(axis.origin)
    rotate(angle, axis.direction)
    add(axis.origin)
}

fun mix(a: Float, b: Float, t: Float) = a + (b - a) * t
fun mix(a: Double, b: Double, t: Double) = a + (b - a) * t

interface SphereCameraProjectionResult {
    val center: Vec2d
    val majorRadius: Double
    val minorRadius: Double
    val area: Double
}

fun Camera.projectSphere(center: Vec3d, radius: Double) = when (this) {
    is OrthographicCamera -> {
        object : SphereCameraProjectionResult {
            override val center = MutableVec3d().also { project(center, it) }.xy
            override val majorRadius = radius / (abs((top - bottom).toDouble()) / 2.0)
            override val minorRadius = majorRadius
            override val area by lazy { PI * majorRadius * minorRadius }
        }
    }

    is PerspectiveCamera -> {
        // https://iquilezles.org/articles/sphereproj/
        val o = view.transform(center, 1.0, MutableVec3d())
        o.z = -o.z
        val r2 = radius * radius
        val z2 = o.z * o.z
        val l2 = o.sqrLength()
        val fle = 1.0 / tan(fovY.rad.toDouble() / 2.0)
        object : SphereCameraProjectionResult {
            override val center by lazy { o.xy * fle * o.z / (z2 - r2) }
            override val majorRadius by lazy { fle * sqrt(-r2 * (r2 - l2) / ((l2 - z2) * (r2 - z2) * (r2 - z2))) * o.xy.length() }
            override val minorRadius by lazy { fle * sqrt(-r2 * (r2 - l2) / ((l2 - z2) * (r2 - z2) * (r2 - l2))) * o.xy.length() }
            override val area by lazy { -PI * fle * fle * r2 * sqrt(abs((l2 - r2) / (r2 - z2))) / (r2 - z2) }
        }
    }

    else -> throw NotImplementedError("$this")
}