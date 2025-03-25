package utils

object AstroConsts {
    /** 100 * 365.25 * 24 * 3600 */
    const val J2000_SECONDS_IN_CENTURY = 3155760000L
}

inline val IntFract.j2000Centuries
    get() = int / AstroConsts.J2000_SECONDS_IN_CENTURY +
        (fract + int.mod(AstroConsts.J2000_SECONDS_IN_CENTURY)) / AstroConsts.J2000_SECONDS_IN_CENTURY