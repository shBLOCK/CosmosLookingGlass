@file:Suppress("NOTHING_TO_INLINE")

package utils

import de.fabmax.kool.math.Vec2d

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