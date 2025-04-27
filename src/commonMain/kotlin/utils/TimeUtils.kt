package utils

import kotlinx.datetime.Instant
import kotlin.jvm.JvmInline

sealed interface IntFractTime {
    val value: IntFract

    @JvmInline
    value class J2000(override val value: IntFract) : IntFractTime {
        companion object {
            const val DAY = 24L * 3600L
            /** 365.25 days */
            const val YEAR = 31557600L
            const val CENTURY = 100L * YEAR
        }

        inline val days get() = value / DAY
        inline val years get() = value / YEAR
        inline val centuries get() = value / CENTURY

        inline val utc get() = UTC(value + J2000_UTC_OFFSET)
    }

    @JvmInline
    value class UTC(override val value: IntFract) : IntFractTime {
        inline val instant
            get() = Instant.fromEpochSeconds(value.int, (value.fract * 1e9).toInt())
        val isDistant get() = this <= Instant.DISTANT_PAST || this >= Instant.DISTANT_FUTURE
        val feasibleInstant get() = if (isDistant) null else instant

        inline val j2000 get() = J2000(value - J2000_UTC_OFFSET)

        operator fun compareTo(instant: Instant) =
            value.int.compareTo(instant.epochSeconds)
                .let { if (it == 0) value.fract.compareTo(instant.nanosecondsOfSecond / 1e9) else it }
    }

    companion object {
        /**
         * According to: https://en.wikipedia.org/wiki/Epoch_(astronomy)#cite_ref-clock24_12-0:~:text=January%201%2C%202000%2C%2011%3A58%3A55.816%20UTC%20(Coordinated%20Universal%20Time).
         */
        val J2000_UTC_OFFSET = Instant.parse("2000-01-01T11:58:55.816Z").epochSecondsIntFract
    }
}

inline val IntFract.j2000 get() = IntFractTime.J2000(this)
inline val IntFract.utc get() = IntFractTime.UTC(this)
inline val Instant.epochSecondsIntFract get() = IntFract(epochSeconds, nanosecondsOfSecond / 1e9)