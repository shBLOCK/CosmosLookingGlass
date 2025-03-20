package universe

import de.fabmax.kool.pipeline.RenderPass
import de.fabmax.kool.scene.Node
import de.fabmax.kool.scene.TrsTransformD
import de.fabmax.kool.util.Color
import dynamics.CelestialDynModel

abstract class CelestialBody(
    val themeColor: Color = Color.WHITE
) : Node() {
    init {
        transform = TrsTransformD() // use double precision
    }

    var dynModelProvider: (() -> CelestialDynModel) = { CelestialDynModel.Static() }
    val dynModel get() = dynModelProvider()

    open fun applyDynModel() {
        transform.setCompositionOf(dynModel.position(), dynModel.orientation())
    }

    override fun update(updateEvent: RenderPass.UpdateEvent) {
        applyDynModel()
        super.update(updateEvent)
    }
}