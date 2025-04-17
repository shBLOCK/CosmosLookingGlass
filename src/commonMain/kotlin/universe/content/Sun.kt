package universe.content

import universe.SingletonCelestialBody

class Sun : SingletonCelestialBody<Sun, Sun.Companion>() {
    override val themeColor = SolarSystemConsts.SUN_THEME_COLOR

    init {
        setupSimpleSpherical(SolarSystemConsts.SUN_RADIUS, SolarSystemConsts.SUN_FLATTENING)
    }

    companion object : CompanionObj<Sun>();
    override val companion get() = Companion
}