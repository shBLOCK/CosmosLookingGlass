package dynamics

import utils.AstroTime
import de.fabmax.kool.math.QuatD
import de.fabmax.kool.math.Vec3d

interface DynModel {
    val time: AstroTime

    fun seek(time: AstroTime)

    interface IPosition {
        fun position(): Vec3d
    }

    interface Position : DynModel, IPosition {
        class Static(private val value: Vec3d) : DynModelBase(), Position {
            override fun position() = value
        }
    }

    interface IOrientation {
        fun orientation(): QuatD
    }

    interface Orientation : DynModel, IOrientation {
        class Static(private val value: QuatD) : DynModelBase(), Orientation {
            override fun orientation() = value
        }
    }
}

abstract class DynModelBase : DynModel {
    override var time: AstroTime = AstroTime(0.0)
        protected set

    override fun seek(time: AstroTime) {
        this.time = time
    }
}