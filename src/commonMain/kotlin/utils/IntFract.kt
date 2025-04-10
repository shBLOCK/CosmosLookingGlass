@file:Suppress("NOTHING_TO_INLINE", "OVERRIDE_BY_INLINE", "ConstPropertyName")

package utils

import kotlin.math.floor
import kotlin.math.nextDown

/**
 * Used to represent big real numbers.
 *
 * When used to represent astronomical time, usually means seconds relative to `J2000.0`.
 */
class IntFract(val int: Long, val fract: Double) {
    constructor(value: Double) : this(floor(value).toLong(), value.mod(1.0))
    constructor(value: Long) : this(value, 0.0)

    inline val rounded get() = if (fract >= 0.5) int + 1 else int

    inline operator fun plus(value: Double) = normalizing(int, fract + value)
    inline operator fun minus(value: Double) = normalizing(int, fract - value)
    inline operator fun plus(other: IntFract) = normalizing(int + other.int, fract + other.fract)
    inline operator fun minus(other: IntFract) = normalizing(int - other.int, fract - other.fract)
    inline operator fun div(value: Int) = int / value + (int % value + fract) / value
    inline operator fun div(value: Long) = int / value + (int % value + fract) / value

    override inline operator fun equals(other: Any?) =
        if (other is IntFract) {
            int == other.int
                && fract == other.fract
        } else false

    inline operator fun compareTo(other: IntFract) =
        int.compareTo(other.int)
            .let { if (it == 0) fract.compareTo(other.fract) else it }

    override inline fun hashCode() = int.hashCode() xor fract.hashCode()

    override fun toString() = "($int+$fract)"

    inline fun toDouble() = int + fract

    companion object {
        val ZERO = IntFract(0)
        val MIN = IntFract(Long.MIN_VALUE, 0.0)
        val MAX = IntFract(Long.MAX_VALUE, 1.0.nextDown())

        inline fun normalizing(int: Long, fract: Double) =
            IntFract(int + floor(fract).toLong(), fract.mod(1.0))
    }
}

inline fun IntFract.floorDiv(other: Int) = int.floorDiv(other)
inline fun IntFract.floorDiv(other: Long) = int.floorDiv(other)

inline fun IntFract.ceilDiv(other: Int) = int.floorDiv(other) + if (int % other == 0L && fract == 0.0) 0L else 1L
inline fun IntFract.ceilDiv(other: Long) = int.floorDiv(other) + if (int % other == 0L && fract == 0.0) 0L else 1L

inline fun IntFract.mod(other: Int) = int.mod(other) + fract
inline fun IntFract.mod(other: Long) = int.mod(other) + fract

inline fun min(a: IntFract, b: IntFract) = if (a < b) a else b
inline fun max(a: IntFract, b: IntFract) = if (a > b) a else b
