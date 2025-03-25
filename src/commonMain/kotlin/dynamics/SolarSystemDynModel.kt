package dynamics

import utils.IntFract

abstract class SolarSystemDynModel : DynModelBase() {
    abstract val sun: CelestialDynModel
    abstract val mercury: CelestialDynModel
    abstract val venus: CelestialDynModel
    abstract val earth: CelestialDynModel
    abstract val moon: CelestialDynModel
    abstract val mars: CelestialDynModel
    abstract val jupiter: CelestialDynModel
    abstract val saturn: CelestialDynModel
    abstract val uranus: CelestialDynModel
    abstract val neptune: CelestialDynModel

    override fun seek(time: IntFract) {
        if (this.time == time) return
        sun.seek(time)
        mercury.seek(time)
        venus.seek(time)
        earth.seek(time)
        moon.seek(time)
        mars.seek(time)
        jupiter.seek(time)
        saturn.seek(time)
        uranus.seek(time)
        neptune.seek(time)
        super.seek(time)
    }
}