package universe.content

import universe.SingletonCelestialBody

class Earth : SingletonCelestialBody<Earth, Earth.Companion>() {
    override val themeColor = SolarSystemConsts.EARTH_THEME_COLOR

    init {
        setupSimpleSpherical(SolarSystemConsts.EARTH_RADIUS, SolarSystemConsts.EARTH_FLATTENING)
    }

    companion object : CompanionObj<Earth>();
    override val companion get() = Companion
}