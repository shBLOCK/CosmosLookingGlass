package dynamics

import de.fabmax.kool.math.MutableVec3d
import de.fabmax.kool.math.Vec3d
import utils.SPEED_OF_LIGHT

/** Calculate the position of a celestial body by solving `barycenter = center of mass`.
 *
 *  Assuming the resulting position would be close to the barycenter.
 */
class BarycenterPositionModel(
    /** Pair<model, mass> */
    val objects: Collection<Pair<DynModel.Position, Double>>,
    val mass: Double,
    val barycenter: Vec3d = Vec3d.ZERO,
    val gravityPropagationSpeed: Double = SPEED_OF_LIGHT
) : DynModelBase(), DynModel.Position {
    override fun position(result: MutableVec3d): MutableVec3d {
        result.set(Vec3d.ZERO)
        val tmpVec3d = MutableVec3d()
        objects.forEach { (model, objMass) ->
            model.seek(time)
            if (gravityPropagationSpeed > 0.0)
                model.seek(time - model.position(tmpVec3d).distance(barycenter) / gravityPropagationSpeed)
            result.add(model.position(tmpVec3d).subtract(barycenter).mul(objMass))
        }
        return result.apply { divAssign(mass) }.add(barycenter)
    }

    override fun copy() = BarycenterPositionModel(
        objects.map { (model, objMass) -> model.copyTyped() to objMass },
        mass,
        barycenter,
        gravityPropagationSpeed
    )
}