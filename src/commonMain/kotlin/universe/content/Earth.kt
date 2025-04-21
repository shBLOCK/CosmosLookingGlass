package universe.content

import universe.SingletonCelestialBody

class Earth : SingletonCelestialBody<Earth, Earth.Companion>() {
    override val themeColor = SolarSystemConsts.EARTH_THEME_COLOR
    override val outlineRadius = SolarSystemConsts.EARTH_RADIUS

    init {
//        setupSimpleSpherical(SolarSystemConsts.EARTH_RADIUS, SolarSystemConsts.EARTH_FLATTENING)
        setupTexturedSpherical(
            SolarSystemConsts.EARTH_RADIUS, SolarSystemConsts.EARTH_FLATTENING,
            "textures/celestial_body/earth/color.png"
        )
    }

    companion object : CompanionObj<Earth>();
    override val companion get() = Companion
}