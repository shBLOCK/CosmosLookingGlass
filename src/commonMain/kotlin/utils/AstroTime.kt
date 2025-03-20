package utils

import kotlin.math.floor

/** 100 * 365.25 * 24 * 3600 */
private const val SECONDS_IN_CENTURY = 3155760000L

/**
 * Represents an **immutable** astronomical time in seconds relative to J2000.0
 *
 * Uses the long + fraction approach to avoid floating-point problems.
 */
class AstroTime(
    seconds: Long,
    fraction: Double
) {
    private val _seconds = seconds + floor(fraction).toLong()
    private val _fraction = fraction.mod(1.0)

    constructor(t: Double) : this(0L, t)
    constructor(t: Long) : this(t, 0.0)

    val nearestSecond get() = if (_fraction >= 0.5) _seconds + 1 else _seconds
    val centuries get() = _seconds / SECONDS_IN_CENTURY +
            (_fraction + _seconds.mod(SECONDS_IN_CENTURY)) / SECONDS_IN_CENTURY

    operator fun plus(delta: Double) =
        AstroTime(_seconds, _fraction + delta)

    operator fun minus(delta: Double) = plus(-delta)

    override operator fun equals(other: Any?) =
        if (other is AstroTime) {
            _seconds == other._seconds
                && _fraction == other._fraction
        } else false

    operator fun compareTo(other: AstroTime) =
        _seconds.compareTo(other._seconds)
            .let { if (it == 0) _fraction.compareTo(other._fraction) else it }

    override fun hashCode() = _seconds.hashCode() xor centuries.hashCode()
}