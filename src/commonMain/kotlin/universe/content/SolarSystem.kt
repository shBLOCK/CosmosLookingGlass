package universe.content

import universe.Universe

class SolarSystem : Universe() {
    init {
        name = "SolarSystem"
        this += Sun()
        this += Mercury()
        this += Venus()
        this += Earth()
        this += Moon()
        this += Mars()
        this += Jupiter()
        this += Saturn()
        this += Uranus()
        this += Neptune()
    }
}