@file:Suppress("NOTHING_TO_INLINE")

package utils

import de.fabmax.kool.math.Vec2d
import de.fabmax.kool.math.Vec3d
import de.fabmax.kool.math.Vec3f
import de.fabmax.kool.math.toVec3f
import de.fabmax.kool.modules.ksl.KslUnlitShader
import de.fabmax.kool.scene.Node
import de.fabmax.kool.scene.addLineMesh
import de.fabmax.kool.util.Color
import de.fabmax.kool.util.Releasable
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty

data class FwdInvFunction<A : Any?, B : Any?>(val forward: (A) -> B, val inverse: (B) -> A) {
    inline operator fun invoke(value: A) = forward(value)

    companion object {
        fun <T : Any?> identity() = FwdInvFunction<T, T>({ it }, { it })
    }
}

inline val OpenEndRange<Double>.width get() = endExclusive - start
inline val ClosedRange<Double>.width get() = endInclusive - start

@PublishedApi
internal val VEC2D_NAN = Vec2d(Double.NaN)
inline val Vec2d.Companion.NaN get() = VEC2D_NAN

class ReleasableImpl(val onRelease: () -> Unit) : Releasable {
    override var isReleased = false

    override fun release() {
        isReleased = true
        onRelease()
    }
}

inline fun <T> Iterable<T>.greedyAny(predicate: (T) -> Boolean): Boolean {
    if (this is Collection && isEmpty()) return false
    var any = false
    for (element in this) any = any || predicate(element)
    return any
}

fun <E> MutableIterator<E>.removeAll() {
    while (hasNext()) {
        next()
        remove()
    }
}

class CombinedProperty<T>(val primary: KMutableProperty0<T>, vararg val others: KMutableProperty0<T>) {
    inline operator fun getValue(thisRef: Any?, property: KProperty<*>) = primary.get()
    inline operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        primary.set(value)
        others.forEach { it.set(value) }
    }
}

fun <T> KMutableProperty0<T>.combinedWith(vararg others: KMutableProperty0<T>) = CombinedProperty(this, *others)

inline fun unreachable(): Nothing = error("unreachable")

fun Node.addDebugAxisIndicator(length: Double) =
    addLineMesh {
        for (axis in arrayOf(Vec3d.X_AXIS, Vec3d.Y_AXIS, Vec3d.Z_AXIS)) {
            addLine(
                Vec3f.ZERO, (axis * length).toVec3f(),
                when (axis) {
                    Vec3d.X_AXIS -> Color.RED
                    Vec3d.Y_AXIS -> Color.GREEN
                    Vec3d.Z_AXIS -> Color.BLUE
                    else -> unreachable()
                }
            )
        }

        shader = KslUnlitShader {
            color { vertexColor() }
        }
    }