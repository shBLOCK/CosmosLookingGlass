@file:Suppress("NOTHING_TO_INLINE", "OVERRIDE_BY_INLINE", "ConstPropertyName")

package utils

import kotlin.math.floor
import kotlin.math.nextDown

/**
 * Represents an **immutable** astronomical time in seconds relative to J2000.0
 *
 * Uses the long + fraction approach to avoid floating-point problems.
 */
class AstroTime(
    seconds: Long,
    fraction: Double
) {
    val seconds = seconds + floor(fraction).toLong()
    val fraction = fraction.mod(1.0)

    constructor(t: Double) : this(0L, t)
    constructor(t: Long) : this(t, 0.0)

    inline val nearestSecond get() = if (fraction >= 0.5) seconds + 1 else seconds
    inline val centuries
        get() = seconds / SECONDS_IN_CENTURY +
            (fraction + seconds.mod(SECONDS_IN_CENTURY)) / SECONDS_IN_CENTURY

    inline operator fun plus(delta: Double) =
        AstroTime(seconds, fraction + delta)

    inline operator fun minus(delta: Double) = plus(-delta)

    override inline operator fun equals(other: Any?) =
        if (other is AstroTime) {
            seconds == other.seconds
                && fraction == other.fraction
        } else false

    inline operator fun compareTo(other: AstroTime) =
        seconds.compareTo(other.seconds)
            .let { if (it == 0) fraction.compareTo(other.fraction) else it }

    override fun hashCode() = seconds.hashCode() xor centuries.hashCode()

    companion object {
        val MIN = AstroTime(Long.MIN_VALUE, 0.0)
        val MAX = AstroTime(Long.MAX_VALUE, 1.0.nextDown())

        /** 100 * 365.25 * 24 * 3600 */
        const val SECONDS_IN_CENTURY = 3155760000L
    }
}