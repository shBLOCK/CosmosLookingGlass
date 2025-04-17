package universe

import de.fabmax.kool.pipeline.RenderPass
import de.fabmax.kool.scene.Scene
import de.fabmax.kool.util.Releasable
import dynamics.UniverseDynModel
import utils.IntFract
import utils.ReleasableImpl

open class Universe : AbstractMutableSet<CelestialBody>() {
    val scene = object : Scene("Universe") {
        init {
            mainRenderPass.isDoublePrecision = true
        }

        override fun update(updateEvent: RenderPass.UpdateEvent) {
            this@Universe.update(updateEvent)
            super.update(updateEvent)
        }
    }

    var name by scene::name

    var time = IntFract(0)

    private val celestialBodies: MutableSet<CelestialBody> = mutableSetOf()
    private val singletonsCelestialBodies: MutableMap<SingletonCelestialBody.CompanionObj<*>, SingletonCelestialBody<*, *>> =
        mutableMapOf()
    private val celestialBodyReleaseListeners = mutableMapOf<CelestialBody, Releasable>()

    var dynModel: UniverseDynModel? = null
        set(value) {
            field?.also { old -> this.forEach(old::releaseDynModelFor) }
            value?.also { new -> this.forEach(new::addDynModelFor) }
            field = value
            value?.seek(time)
        }

    override fun add(element: CelestialBody): Boolean {
        if (element is SingletonCelestialBody<*, *>) {
            singletonsCelestialBodies[element.companion]?.also(::remove)
            singletonsCelestialBodies[element.companion] = element
        }
        if (!celestialBodies.add(element)) return false
        scene += element
        dynModel?.addDynModelFor(element)
        val releaseListener = ReleasableImpl { remove(element) }
        celestialBodyReleaseListeners[element] = releaseListener
        element.addDependingReleasable(releaseListener)
        return true
    }

    override fun remove(element: CelestialBody): Boolean {
        if (celestialBodies.remove(element)) {
            scene -= element
            dynModel?.releaseDynModelFor(element)
            if (element is SingletonCelestialBody<*, *>)
                singletonsCelestialBodies -= element.companion
            celestialBodyReleaseListeners.remove(element)
            return true
        }
        return false
    }

    @Suppress("UNCHECKED_CAST")
    operator fun <CP : SingletonCelestialBody.CompanionObj<CLS>, CLS : SingletonCelestialBody<CLS, CP>> get(companion: CP): CLS? =
        singletonsCelestialBodies[companion]?.let { it as CLS }

    private fun update(updateEvent: RenderPass.UpdateEvent) {
        dynModel?.seek(time)
    }

    override val size by celestialBodies::size
    override fun iterator() = celestialBodies.iterator()
    override fun contains(element: CelestialBody) = celestialBodies.contains(element)
    override fun clear() = celestialBodies.clear()
}