package universe

import de.fabmax.kool.util.Color

/**
 * Unless otherwise noticed, celestial body facts source: [https://nssdc.gsfc.nasa.gov/planetary/planetfact.html](https://nssdc.gsfc.nasa.gov/planetary/planetfact.html)
 *
 * Theme Color: [https://imgur.com/a/OCCq2gl](https://imgur.com/a/OCCq2gl)
 */
object SolarSystemConsts {
    //TODO: use wikipedia sources data (replace 1638237449047_solar-system-datatable.pdf datas)

    const val SUN_RADIUS = 695700e3 // https://en.wikipedia.org/wiki/Sun
    const val SUN_FLATTENING = 0.00005

    const val MERCURY_RADIUS = 2440.5e3
    const val MERCURY_FLATTENING = 0.0009

    const val VENUS_RADIUS = 6051.8e3
    const val VENUS_FLATTENING = 0.000

    const val EARTH_RADIUS = 6378.137e3
    const val EARTH_FLATTENING = 0.003353

    const val MOON_RADIUS = 1738.1e3
    const val MOON_FLATTENING = 0.0012

    const val MARS_RADIUS = 3396.2e3
    const val MARS_FLATTENING = 0.00589

    const val JUPITER_RADIUS = 71492e3 // 1 bar level
    const val JUPITER_FLATTENING = 0.06487

    const val SATURN_RADIUS = 60268e3 // 1 bar level
    const val SATURN_FLATTENING = 0.09796

    const val URANUS_RADIUS = 25559e3 // 1 bar level
    const val URANUS_FLATTENING = 0.02293

    const val NEPTUNE_RADIUS = 24764e3 // 1 bar level
    const val NEPTUNE_FLATTENING = 0.01708

    const val PLUTO_RADIUS = 1188e3
    const val PLUTO_FLATTENING = 0.0000

    val SUN_THEME_COLOR = Color("F15D22")
    val MERCURY_THEME_COLOR = Color("BFBDBC")
    val VENUS_THEME_COLOR = Color("F3DBC3")
    val EARTH_THEME_COLOR = Color("1F386F")
    val MOON_THEME_COLOR = Color("F4F6F8")
    val MARS_THEME_COLOR = Color("C36D5C")
    val JUPITER_THEME_COLOR = Color("BFB09C")
    val SATURN_THEME_COLOR = Color("DAB778")
    val URANUS_THEME_COLOR = Color("CFECF0")
    val NEPTUNE_THEME_COLOR = Color("789EBF")
}