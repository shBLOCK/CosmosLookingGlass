package dynamics

import universe.CelestialBody
import utils.IntFract

abstract class UniverseDynModel : DynModelBase() {
    abstract fun addDynModelFor(celestialBody: CelestialBody): Boolean
    abstract fun releaseDynModelFor(celestialBody: CelestialBody)
    abstract operator fun contains(celestialBody: CelestialBody): Boolean
}

abstract class UniverseDynModelImpl : UniverseDynModel() {
    private val activeModels = mutableMapOf<CelestialBody, CelestialDynModel>()

    open fun getDynModelFor(celestialBody: CelestialBody): CelestialDynModel? = null

    final override fun addDynModelFor(celestialBody: CelestialBody): Boolean {
        check(celestialBody !in activeModels) { "Already added: $celestialBody" }
        val model = getDynModelFor(celestialBody) ?: return false
        celestialBody.dynModel = model
        activeModels[celestialBody] = model
        return true
    }

    final override fun releaseDynModelFor(celestialBody: CelestialBody) {
        activeModels.remove(celestialBody) ?: throw IllegalStateException("Not added: $celestialBody")
        celestialBody.dynModel = null
    }

    final override fun contains(celestialBody: CelestialBody) = celestialBody in activeModels

    override fun seek(time: IntFract) {
        super.seek(time)
        activeModels.values.forEach { it.seek(time) }
    }
}