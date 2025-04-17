package universe.content

import universe.SingletonCelestialBody

class Mercury : SingletonCelestialBody<Mercury, Mercury.Companion>() {
    override val themeColor = SolarSystemConsts.MERCURY_THEME_COLOR

    init {
        setupSimpleSpherical(SolarSystemConsts.MERCURY_RADIUS, SolarSystemConsts.MERCURY_FLATTENING)
    }

    companion object : CompanionObj<Mercury>();
    override val companion get() = Companion
}

class Venus : SingletonCelestialBody<Venus, Venus.Companion>() {
    override val themeColor = SolarSystemConsts.VENUS_THEME_COLOR

    init {
        setupSimpleSpherical(SolarSystemConsts.VENUS_RADIUS, SolarSystemConsts.VENUS_FLATTENING)
    }

    companion object : CompanionObj<Venus>();
    override val companion get() = Companion
}

class Mars : SingletonCelestialBody<Mars, Mars.Companion>() {
    override val themeColor = SolarSystemConsts.MARS_THEME_COLOR

    init {
        setupSimpleSpherical(SolarSystemConsts.MARS_RADIUS, SolarSystemConsts.MARS_FLATTENING)
    }

    companion object : CompanionObj<Mars>();
    override val companion get() = Companion
}

class Jupiter : SingletonCelestialBody<Jupiter, Jupiter.Companion>() {
    override val themeColor = SolarSystemConsts.JUPITER_THEME_COLOR

    init {
        setupSimpleSpherical(SolarSystemConsts.JUPITER_RADIUS, SolarSystemConsts.JUPITER_FLATTENING)
    }

    companion object : CompanionObj<Jupiter>();
    override val companion get() = Companion
}

class Saturn : SingletonCelestialBody<Saturn, Saturn.Companion>() {
    override val themeColor = SolarSystemConsts.SATURN_THEME_COLOR

    init {
        setupSimpleSpherical(SolarSystemConsts.SATURN_RADIUS, SolarSystemConsts.SATURN_FLATTENING)
    }

    companion object : CompanionObj<Saturn>();
    override val companion get() = Companion
}

class Uranus : SingletonCelestialBody<Uranus, Uranus.Companion>() {
    override val themeColor = SolarSystemConsts.URANUS_THEME_COLOR

    init {
        setupSimpleSpherical(SolarSystemConsts.URANUS_RADIUS, SolarSystemConsts.URANUS_FLATTENING)
    }

    companion object : CompanionObj<Uranus>();
    override val companion get() = Companion
}

class Neptune : SingletonCelestialBody<Neptune, Neptune.Companion>() {
    override val themeColor = SolarSystemConsts.NEPTUNE_THEME_COLOR

    init {
        setupSimpleSpherical(SolarSystemConsts.NEPTUNE_RADIUS, SolarSystemConsts.NEPTUNE_FLATTENING)
    }

    companion object : CompanionObj<Neptune>();
    override val companion get() = Companion
}
