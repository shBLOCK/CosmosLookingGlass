package universe

import de.fabmax.kool.math.toVec3d
import de.fabmax.kool.pipeline.DepthMode
import de.fabmax.kool.scene.Scene
import de.fabmax.kool.scene.TrsTransformD
import de.fabmax.kool.scene.addGroup
import de.fabmax.kool.util.BaseReleasable
import de.fabmax.kool.util.Releasable
import dynamics.UniverseDynModel
import utils.IntFract
import utils.MutableCollectionMixin
import utils.ReleasableImpl

open class Universe : MutableSet<CelestialBody>, MutableCollectionMixin<CelestialBody>, BaseReleasable() {
    val scene = object : Scene("Universe") {
        init {
            addDependingReleasable(this@Universe)
            mainRenderPass.isDoublePrecision = true
            mainRenderPass.depthMode = DepthMode.Legacy
        }
    }
    val root = scene.addGroup("Root") {
        transform = TrsTransformD() // use double precision transform
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

    fun update() {
        dynModel?.seek(time)
        forEach { it.update() }
    }

    override fun add(element: CelestialBody): Boolean {
        if (element is SingletonCelestialBody<*, *>) {
            singletonsCelestialBodies[element.companion]?.also(::remove)
            singletonsCelestialBodies[element.companion] = element
        }
        if (!celestialBodies.add(element)) return false
        root += element
        dynModel?.addDynModelFor(element)
        val releaseListener = ReleasableImpl { remove(element) }
        celestialBodyReleaseListeners[element] = releaseListener
        element.addDependingReleasable(releaseListener)
        return true
    }

    override fun remove(element: CelestialBody): Boolean {
        if (celestialBodies.remove(element)) {
            root -= element
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

    override fun release() {
        removeDependingReleasable(scene) // avoid infinite recursion
        // Release (thus remove) all celestial bodies first to avoid their release listeners triggering remove
        // during iteration of the children list in super.release() causing ConcurrentModificationException.
        celestialBodies.toList().forEach { it.release() }
        scene.release()
        super.release()
    }

    /**
     * Apply a global translation to keep the camera at the origin to avoid float precision problems on GPU side.
     */
    fun setGlobalTransformHackForFloatPrecision() {
        if (scene.camera.parent != root)
            root += scene.camera
        root.transform.setIdentity().scale(1.0).translate(scene.camera.position.toVec3d() * -1.0)
    }

    fun resetGlobalTransformHackForFloatPrecision() {
        root.transform.setIdentity()
        root.updateModelMatRecursive()
    }

    override val size by celestialBodies::size
    override fun iterator() = celestialBodies.iterator()
    override fun contains(element: CelestialBody) = celestialBodies.contains(element)
    override fun clear() = celestialBodies.clear()
}