@file:Suppress("NOTHING_TO_INLINE")

package dynamics

import de.fabmax.kool.math.MutableQuatD
import de.fabmax.kool.math.MutableVec3d
import utils.IntFract
import de.fabmax.kool.math.QuatD
import de.fabmax.kool.math.Vec3d

sealed interface DynModel {
    val time: IntFract

    fun seek(time: IntFract)

    fun copy(): DynModel

    interface IPosition {
        fun position(result: MutableVec3d = MutableVec3d()): MutableVec3d
    }

    interface Position : DynModel, IPosition {
        class Static(private val value: Vec3d) : DynModelBase(), Position {
            override fun position(result: MutableVec3d) = result.set(value)

            override fun copy() = this
        }
    }

    interface IOrientation {
        fun orientation(result: MutableQuatD = MutableQuatD()): MutableQuatD
    }

    interface Orientation : DynModel, IOrientation {
        class Static(private val value: QuatD) : DynModelBase(), Orientation {
            override fun orientation(result: MutableQuatD): MutableQuatD = result.set(value)

            override fun copy() = this
        }
    }
}

abstract class DynModelBase : DynModel {
    override var time = IntFract(0.0)
        protected set

    override fun seek(time: IntFract) {
        this.time = time
    }
}

inline fun <reified T: DynModel> T.copyTyped() = copy() as T