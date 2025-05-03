@file:Suppress("NOTHING_TO_INLINE", "OVERRIDE_BY_INLINE", "SortModifiers")

package utils

import de.fabmax.kool.math.*
import de.fabmax.kool.pipeline.RenderPass
import de.fabmax.kool.scene.Camera
import de.fabmax.kool.scene.OrthographicCamera
import de.fabmax.kool.scene.PerspectiveCamera
import de.fabmax.kool.util.Time
import de.fabmax.kool.util.Viewport
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

fun MutableQuatD.expDecaySnapping(target: QuatD, decay: Double, deltaT: Float = Time.deltaT): MutableQuatD {
    if (isFuzzyEqual(target)) {
        set(target)
    } else {
        mix(target, exp(-decay * deltaT), result = this)
    }
    return norm()
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

inline fun sqr(x: Float) = x * x
inline fun sqr(x: Double) = x * x

fun mix(a: Float, b: Float, t: Float) = a + (b - a) * t
fun mix(a: Double, b: Double, t: Double) = a + (b - a) * t

interface SphereCameraProjectionResult {
    val center: Vec2d
    val centerDistance: Double
    val majorRadius: Double
    val minorRadius: Double
    val area: Double
}

class ViewportSphereCameraProjectionResult(
    val viewport: Viewport,
    @PublishedApi internal val org: SphereCameraProjectionResult
) : SphereCameraProjectionResult {
    override val center
        get() = MutableVec2d(org.center).apply {
            y = -y
            mul(viewport.height / 2.0)
            x += viewport.width / 2.0
            y += viewport.height / 2.0
        }
    inline override val centerDistance get() = org.centerDistance
    inline override val majorRadius get() = org.majorRadius * (viewport.height / 2.0)
    inline override val minorRadius get() = org.minorRadius * (viewport.height / 2.0)
    inline override val area get() = org.area * sqr(viewport.height / 2.0)
}

inline fun SphereCameraProjectionResult.on(viewport: Viewport) =
    ViewportSphereCameraProjectionResult(viewport, this)

fun Camera.projectSphere(center: Vec3d, radius: Double) = when (this) {
    is OrthographicCamera -> {
        object : SphereCameraProjectionResult {
            override val center = MutableVec3d().also { project(center, it) }.xy
            override val centerDistance = center dot dataD.globalLookDir.normed()
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
            override val centerDistance by lazy { sqrt(l2).withSign(o.z) }
            override val majorRadius by lazy { fle * sqrt(-r2 * (r2 - l2) / ((l2 - z2) * (r2 - z2) * (r2 - z2))) * o.xy.length() }
            override val minorRadius by lazy { fle * sqrt(-r2 * (r2 - l2) / ((l2 - z2) * (r2 - z2) * (r2 - l2))) * o.xy.length() }
            override val area by lazy { -PI * fle * fle * r2 * sqrt(abs((l2 - r2) / (r2 - z2))) / (r2 - z2) }
        }
    }

    else -> throw NotImplementedError("$this")
}

inline fun Camera.projectSphere(sphere: Sphere) = projectSphere(sphere.center, sphere.radius)
inline fun RenderPass.View.projectSphereViewport(sphere: Sphere) = camera.projectSphere(sphere).on(viewport)

fun rayLineSegmentIntersectionUnchecked(ray: RayD, lineA: Vec3d, lineB: Vec3d): Vec3d {
    if (lineA.isFuzzyEqual(lineB)) return lineA

    val v = lineB - lineA
    val w0 = ray.origin - lineA

    val a = ray.direction.sqrLength()
    val b = ray.direction dot v
    val c = v.sqrLength()
    val d1 = ray.direction.dot(w0)
    val d2 = v.dot(w0)

    val denom = a * c - b * b
    val t = (b * d2 - c * d1) / denom

    return ray.origin + ray.direction * t
}