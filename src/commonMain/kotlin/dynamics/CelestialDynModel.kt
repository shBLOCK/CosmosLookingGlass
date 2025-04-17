@file:Suppress("NOTHING_TO_INLINE")

package dynamics

import de.fabmax.kool.math.MutableQuatD
import de.fabmax.kool.math.MutableVec3d
import de.fabmax.kool.math.QuatD
import de.fabmax.kool.math.Vec3d
import utils.IntFract
import utils.slerpShortest

interface CelestialDynModel : DynModel.Position, DynModel.Orientation {
    open class Composed(
        val positionModel: DynModel.Position,
        val orientationModel: DynModel.Orientation
    ) : DynModelBase(),
        CelestialDynModel,
        DynModel.IPosition by positionModel,
        DynModel.IOrientation by orientationModel {
        override fun seek(time: IntFract) {
            positionModel.seek(time)
            orientationModel.seek(time)
            super.seek(time)
        }

        override fun copy() = Composed(positionModel.copyTyped(), orientationModel.copyTyped())
    }

    sealed class Static(position: Vec3d = Vec3d.ZERO, orientation: QuatD = QuatD.IDENTITY) :
        Composed(DynModel.Position.Static(position), DynModel.Orientation.Static(orientation)) {
        override fun copy() = this
    }

    class Blending(val a: CelestialDynModel, val b: CelestialDynModel, var t: Double = 0.0) :
        DynModelBase(), CelestialDynModel {
        override fun seek(time: IntFract) {
            a.seek(time)
            b.seek(time)
            super<DynModelBase>.seek(time)
        }

        override fun copy() = Blending(a, b, t)

        private val tmpVec3da = MutableVec3d()
        private val tmpVec3db = MutableVec3d()

        override fun position(result: MutableVec3d): MutableVec3d {
            a.position(tmpVec3da)
            b.position(tmpVec3db)
            return tmpVec3da.mix(tmpVec3db, t, result)
        }

        private val tmpQuatDa = MutableQuatD()
        private val tmpQuatDb = MutableQuatD()

        override fun orientation(result: MutableQuatD): MutableQuatD {
            a.orientation(tmpQuatDa)
            b.orientation(tmpQuatDb)
            return slerpShortest(tmpQuatDa, tmpQuatDb, t, result)
        }
    }

    class Dummy : Static()
}