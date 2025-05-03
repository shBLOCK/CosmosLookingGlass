package universe

import de.fabmax.kool.math.MutableVec3d
import de.fabmax.kool.math.QuatD
import de.fabmax.kool.math.Vec3d
import de.fabmax.kool.scene.Node
import de.fabmax.kool.scene.TrsTransformD
import de.fabmax.kool.util.Color
import dynamics.CelestialDynModel
import utils.Sphere

abstract class CelestialBody : Node() {
    init {
        transform = TrsTransformD() // use double precision transform
    }

    var position: Vec3d = Vec3d.ZERO
        private set
    val globalPosition: Vec3d get() = toGlobalCoords(MutableVec3d())
    var orientation: QuatD = QuatD.IDENTITY
        private set

    open val themeColor: Color = Color.WHITE
    open val outlineRadius: Double = 0.0

    var dynModel: CelestialDynModel? = null
        internal set

    open fun update() {
        dynModel?.let { model ->
            position = model.position()
            orientation = model.orientation()
            transform.setCompositionOf(position, orientation)
        }
    }
}

val CelestialBody.globalOutlineSphere
    get() = Sphere(globalPosition, outlineRadius)
val CelestialBody.outlineSphere
    get() = Sphere(position, outlineRadius)