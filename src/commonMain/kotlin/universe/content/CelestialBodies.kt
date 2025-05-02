package universe.content

import de.fabmax.kool.Assets
import platform.platformImg
import universe.SingletonCelestialBody

class Mercury : SingletonCelestialBody<Mercury, Mercury.Companion>() {
    override val themeColor = SolarSystemConsts.MERCURY_THEME_COLOR
    override val outlineRadius = SolarSystemConsts.MERCURY_RADIUS

    init {
//        setupSimpleSpherical(SolarSystemConsts.MERCURY_RADIUS, SolarSystemConsts.MERCURY_FLATTENING)
        setupTexturedSpherical(
            SolarSystemConsts.MERCURY_RADIUS, SolarSystemConsts.MERCURY_FLATTENING,
            Assets.platformImg("textures/celestial_body/mercury/color")
        )
    }

    companion object : CompanionObj<Mercury>();
    override val companion get() = Companion
}

class Venus : SingletonCelestialBody<Venus, Venus.Companion>() {
    override val themeColor = SolarSystemConsts.VENUS_THEME_COLOR
    override val outlineRadius = SolarSystemConsts.VENUS_RADIUS

    init {
//        setupSimpleSpherical(SolarSystemConsts.VENUS_RADIUS, SolarSystemConsts.VENUS_FLATTENING)
        setupTexturedSpherical(
            SolarSystemConsts.VENUS_RADIUS, SolarSystemConsts.VENUS_FLATTENING,
            Assets.platformImg("textures/celestial_body/venus/color")
        )
    }

    companion object : CompanionObj<Venus>();
    override val companion get() = Companion
}

class Mars : SingletonCelestialBody<Mars, Mars.Companion>() {
    override val themeColor = SolarSystemConsts.MARS_THEME_COLOR
    override val outlineRadius = SolarSystemConsts.MARS_RADIUS

    init {
//        setupSimpleSpherical(SolarSystemConsts.MARS_RADIUS, SolarSystemConsts.MARS_FLATTENING)
        setupTexturedSpherical(
            SolarSystemConsts.MARS_RADIUS, SolarSystemConsts.MARS_FLATTENING,
            Assets.platformImg("textures/celestial_body/mars/color")
        )
    }

    companion object : CompanionObj<Mars>();
    override val companion get() = Companion
}

class Jupiter : SingletonCelestialBody<Jupiter, Jupiter.Companion>() {
    override val themeColor = SolarSystemConsts.JUPITER_THEME_COLOR
    override val outlineRadius = SolarSystemConsts.JUPITER_RADIUS

    init {
//        setupSimpleSpherical(SolarSystemConsts.JUPITER_RADIUS, SolarSystemConsts.JUPITER_FLATTENING)
        setupTexturedSpherical(
            SolarSystemConsts.JUPITER_RADIUS, SolarSystemConsts.JUPITER_FLATTENING,
            Assets.platformImg("textures/celestial_body/jupiter/color")
        )
    }

    companion object : CompanionObj<Jupiter>();
    override val companion get() = Companion
}

class Saturn : SingletonCelestialBody<Saturn, Saturn.Companion>() {
    override val themeColor = SolarSystemConsts.SATURN_THEME_COLOR
    override val outlineRadius = SolarSystemConsts.SATURN_RADIUS

    init {
//        setupSimpleSpherical(SolarSystemConsts.SATURN_RADIUS, SolarSystemConsts.SATURN_FLATTENING)
        setupTexturedSpherical(
            SolarSystemConsts.SATURN_RADIUS, SolarSystemConsts.SATURN_FLATTENING,
            Assets.platformImg("textures/celestial_body/saturn/color")
        )
    }

    companion object : CompanionObj<Saturn>();
    override val companion get() = Companion
}

class Uranus : SingletonCelestialBody<Uranus, Uranus.Companion>() {
    override val themeColor = SolarSystemConsts.URANUS_THEME_COLOR
    override val outlineRadius = SolarSystemConsts.URANUS_RADIUS

    init {
//        setupSimpleSpherical(SolarSystemConsts.URANUS_RADIUS, SolarSystemConsts.URANUS_FLATTENING)
        setupTexturedSpherical(
            SolarSystemConsts.URANUS_RADIUS, SolarSystemConsts.URANUS_FLATTENING,
            Assets.platformImg("textures/celestial_body/uranus/color")
        )
    }

    companion object : CompanionObj<Uranus>();
    override val companion get() = Companion
}

class Neptune : SingletonCelestialBody<Neptune, Neptune.Companion>() {
    override val themeColor = SolarSystemConsts.NEPTUNE_THEME_COLOR
    override val outlineRadius = SolarSystemConsts.NEPTUNE_RADIUS

    init {
//        setupSimpleSpherical(SolarSystemConsts.NEPTUNE_RADIUS, SolarSystemConsts.NEPTUNE_FLATTENING)
        setupTexturedSpherical(
            SolarSystemConsts.NEPTUNE_RADIUS, SolarSystemConsts.NEPTUNE_FLATTENING,
            Assets.platformImg("textures/celestial_body/neptune/color")
        )
    }

    companion object : CompanionObj<Neptune>();
    override val companion get() = Companion
}

class Moon : SingletonCelestialBody<Moon, Moon.Companion>() {
    override val themeColor = SolarSystemConsts.MOON_THEME_COLOR
    override val outlineRadius = SolarSystemConsts.MOON_RADIUS

    init {
//        setupSimpleSpherical(SolarSystemConsts.MOON_RADIUS, SolarSystemConsts.MOON_FLATTENING)
        setupTexturedSpherical(
            SolarSystemConsts.MOON_RADIUS, SolarSystemConsts.MOON_FLATTENING,
            Assets.platformImg("textures/celestial_body/moon/color")
        )
    }

    companion object : CompanionObj<Moon>();
    override val companion get() = Companion
}

class Sun : SingletonCelestialBody<Sun, Sun.Companion>() {
    override val themeColor = SolarSystemConsts.SUN_THEME_COLOR
    override val outlineRadius = SolarSystemConsts.SUN_RADIUS

    init {
//        setupSimpleSpherical(SolarSystemConsts.SUN_RADIUS, SolarSystemConsts.SUN_FLATTENING)
        setupTexturedSpherical(
            SolarSystemConsts.SUN_RADIUS, SolarSystemConsts.SUN_FLATTENING,
            Assets.platformImg("textures/celestial_body/sun/color")
        )
    }

    companion object : CompanionObj<Sun>();
    override val companion get() = Companion
}