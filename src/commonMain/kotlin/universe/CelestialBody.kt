package universe

import de.fabmax.kool.scene.Node
import de.fabmax.kool.scene.TrsTransformD
import de.fabmax.kool.util.Color
import dynamics.CelestialDynModel

abstract class CelestialBody : Node() {
    init {
        transform = TrsTransformD() // use double precision transform
    }

    open val themeColor: Color = Color.WHITE
    open val outlineRadius: Double = 0.0

    var dynModel: CelestialDynModel? = null
        internal set

    open fun update() {
        dynModel?.also { transform.setCompositionOf(it.position(), it.orientation()) }
    }
}