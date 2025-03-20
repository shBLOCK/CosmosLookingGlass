package dynamics

import de.fabmax.kool.math.QuatD
import de.fabmax.kool.math.Vec3d
import utils.AstroTime

interface CelestialDynModel : DynModel.Position, DynModel.Orientation {
    open class Composed(
        val positionModel: DynModel.Position,
        val orientationModel: DynModel.Orientation
    ) : DynModelBase(),
        CelestialDynModel,
        DynModel.IPosition by positionModel,
        DynModel.IOrientation by orientationModel {
        override fun seek(time: AstroTime) {
            positionModel.seek(time)
            orientationModel.seek(time)
            super.seek(time)
        }
    }

    class Static(position: Vec3d = Vec3d.ZERO, orientation: QuatD = QuatD.IDENTITY) :
        Composed(DynModel.Position.Static(position), DynModel.Orientation.Static(orientation))
}