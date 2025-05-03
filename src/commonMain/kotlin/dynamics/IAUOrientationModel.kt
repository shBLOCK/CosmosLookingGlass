@file:Suppress("LocalVariableName", "PropertyName")

package dynamics

import de.fabmax.kool.math.MutableQuatD
import de.fabmax.kool.math.Vec3d
import de.fabmax.kool.math.deg
import utils.j2000

class IAUOrientationModel(
    private val paramUpdater: IAUOrientationModel.(d: Double, T: Double) -> Unit
) : DynModel.Orientation, LazyDynModelBase() {
    private val lazyResult = MutableQuatD()

    var a0 = 0.0
    var d0 = 0.0
    var W = 0.0

    override fun orientation(result: MutableQuatD): MutableQuatD {
        lazyPath { return result.set(lazyResult) }

        paramUpdater(time.j2000.days, time.j2000.centuries)
        result.setIdentity()
        // TODO: validate accuracy
        result.rotate((a0 + 90.0).deg, Vec3d.Z_AXIS)
        result.rotate((90.0 - d0).deg, Vec3d.X_AXIS)
        result.rotate(W.deg, Vec3d.Z_AXIS)
        return result
    }

    override fun copy() = IAUOrientationModel(paramUpdater)
}