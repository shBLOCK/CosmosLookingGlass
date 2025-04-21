package universe

import de.fabmax.kool.pipeline.RenderPass
import de.fabmax.kool.scene.Node
import de.fabmax.kool.scene.TrsTransformD
import de.fabmax.kool.util.Color
import dynamics.CelestialDynModel

abstract class CelestialBody : Node() {
    init {
        transform = TrsTransformD() // use double precision
    }

    open val themeColor: Color = Color.WHITE
    open val outlineRadius: Double = 0.0

    var dynModel: CelestialDynModel? = null
        internal set

    open fun applyDynModel() {
        dynModel?.also { transform.setCompositionOf(it.position(), it.orientation()) }
    }

    override fun update(updateEvent: RenderPass.UpdateEvent) {
        applyDynModel()
        super.update(updateEvent)
    }
}