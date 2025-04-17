package universe.content

import universe.SingletonCelestialBody

class Moon : SingletonCelestialBody<Moon, Moon.Companion>() {
    override val themeColor = SolarSystemConsts.MOON_THEME_COLOR

    init {
        setupSimpleSpherical(SolarSystemConsts.MOON_RADIUS, SolarSystemConsts.MOON_FLATTENING)
    }

    companion object : CompanionObj<Moon>();
    override val companion get() = Companion
}