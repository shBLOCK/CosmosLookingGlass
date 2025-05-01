@file:Suppress("NOTHING_TO_INLINE", "OVERRIDE_BY_INLINE")

package utils

import de.fabmax.kool.math.*
import de.fabmax.kool.modules.ksl.KslUnlitShader
import de.fabmax.kool.modules.ui2.UiNode
import de.fabmax.kool.scene.Node
import de.fabmax.kool.scene.addLineMesh
import de.fabmax.kool.util.Color
import de.fabmax.kool.util.Releasable
import de.fabmax.kool.util.Viewport
import kotlin.jvm.JvmInline
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
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

data class RectF(val minX: Float, val minY: Float, val width: Float, val height: Float) {
    inline val maxX get() = minX + width
    inline val maxY get() = minY + height

    inline val min get() = Vec2f(minX, minY)
    inline val max get() = Vec2f(maxX, maxY)
    inline val size get() = Vec2f(width, height)
    inline val area get() = width * height

    operator fun contains(point: Vec2f) = minX <= point.x && minY <= point.y && point.x <= maxX && point.y <= maxY
    operator fun contains(rect: RectF) =
        minX <= rect.minX && minY <= rect.minY && rect.maxX <= maxX && rect.maxY <= maxY

    infix fun overlaps(rect: RectF) = minX <= rect.maxX && rect.minX <= maxX && minY <= rect.maxY && rect.minY <= maxY

    companion object {
        inline fun minmax(minX: Float, minY: Float, maxX: Float, maxY: Float) = RectF(minX, minY, maxX, maxY)
        inline fun minmax(packed: Vec4f) = minmax(packed.x, packed.y, packed.z, packed.w)
        fun overlap(a: RectF, b: RectF) = RectF(
            maxOf(a.minX, b.minX),
            maxOf(a.minY, b.minY),
            maxOf(0f, minOf(a.maxX, b.maxX) - maxOf(a.minX, b.minX)),
            maxOf(0f, minOf(a.maxY, b.maxY) - maxOf(a.minY, b.minY))
        )
    }
}

fun isFuzzyEqual(a: RectF, b: RectF, eps: Float = FUZZY_EQ_F) =
    isFuzzyEqual(a.minX, b.minX, eps) && isFuzzyEqual(a.minY, b.minY, eps)
        && isFuzzyEqual(a.width, b.width, eps) && isFuzzyEqual(a.height, b.height, eps)

inline val Viewport.rect get() = RectF(x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat())
inline val UiNode.rect get() = RectF.minmax(leftPx, topPx, rightPx, bottomPx)
inline val UiNode.clipRect get() = RectF.minmax(clipBoundsPx)

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

inline fun Boolean.toInt() = if (this) 1 else 0
inline fun Boolean.toLong() = if (this) 1L else 0L
inline fun Boolean.toFloat() = if (this) 1F else 0F
inline fun Boolean.toDouble() = if (this) 1.0 else 0.0

data class Sphere(val center: Vec3d, val radius: Double)

class State<T : Any?>(var value: T) : ReadWriteProperty<Any?, T> {
    override inline operator fun getValue(thisRef: Any?, property: KProperty<*>) = value
    override inline operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value = value
    }

    override fun toString() = "State($value)"
}

/** Manages properties that can be (re)initialized on demand. */
class WeakProps {
    private val props = mutableListOf<WeakProp<*>>()

    operator fun <T : Any?> invoke(initializer: () -> T) = WeakProp(initializer).also { props += it }

    fun initialize() = props.forEach { it.initialize() }

    class WeakProp<T : Any?> internal constructor(internal val initializer: () -> T) : ReadWriteProperty<Any?, T> {
        var initialized = false
        var value: T? = null

        internal inline fun initialize() {
            initialized = true
            value = initializer()
        }

        override inline operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
            check(initialized) { "WeakProp <$thisRef>.${property.name} is not initialized yet." }
            @Suppress("UNCHECKED_CAST")
            return value as T
        }

        override inline operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            initialized = true
            this.value = value
        }
    }
}

@JvmInline
value class UnwrappingReadWriteProperty<in T, V>(
    val wrapped: ReadOnlyProperty<T, ReadWriteProperty<T, V>>
) : ReadWriteProperty<T, V> {
    override inline fun getValue(thisRef: T, property: KProperty<*>): V =
        wrapped.getValue(thisRef, property).getValue(thisRef, property)

    override inline fun setValue(thisRef: T, property: KProperty<*>, value: V) =
        wrapped.getValue(thisRef, property).setValue(thisRef, property, value)
}

fun <T, V> ReadOnlyProperty<T, ReadWriteProperty<T, V>>.unwrap() = UnwrappingReadWriteProperty(this)

inline fun <P1, P2, P3, R> ((P1, P2, P3) -> R).curry(param: P1) = { p2: P2, p3: P3 -> this(param, p2, p3) }