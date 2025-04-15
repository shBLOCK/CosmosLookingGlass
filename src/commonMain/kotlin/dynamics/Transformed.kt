package dynamics

import de.fabmax.kool.math.MutableQuatD
import de.fabmax.kool.math.MutableVec3d
import utils.IntFract

class TransformedCelestialDynModel(
    private val transform: Transform,
    private val delegate: CelestialDynModel
) : CelestialDynModel by delegate {

    private var dirty = true
    override fun seek(time: IntFract) {
        delegate.seek(time)
        dirty = true
    }

    private val _position = MutableVec3d()
    private val _orientation = MutableQuatD()

    private fun update() {
        if (dirty) {
            delegate.position(_position)
            delegate.orientation(_orientation)
            transform(_position, _orientation)
            dirty = false
        }
    }

    override fun position(result: MutableVec3d) = update().let { result.set(_position) }
    override fun orientation(result: MutableQuatD) = update().let { result.set(_orientation) }

    override fun copy() = TransformedCelestialDynModel(transform, delegate.copyTyped())

    typealias Transform = (position: MutableVec3d, orientation: MutableQuatD) -> Unit
}

fun CelestialDynModel.transformed(transform: TransformedCelestialDynModel.Transform) =
    TransformedCelestialDynModel(transform, this)

class SolarSystemTransformedDynModel(
    private val transform: TransformedCelestialDynModel.Transform,
    private val delegate: SolarSystemDynModel
) : SolarSystemDynModel() {
    override val sun = TransformedCelestialDynModel(transform, delegate.sun)
    override val mercury = TransformedCelestialDynModel(transform, delegate.mercury)
    override val venus = TransformedCelestialDynModel(transform, delegate.venus)
    override val earth = TransformedCelestialDynModel(transform, delegate.earth)
    override val moon = TransformedCelestialDynModel(transform, delegate.moon)
    override val mars = TransformedCelestialDynModel(transform, delegate.mars)
    override val jupiter = TransformedCelestialDynModel(transform, delegate.jupiter)
    override val saturn = TransformedCelestialDynModel(transform, delegate.saturn)
    override val uranus = TransformedCelestialDynModel(transform, delegate.uranus)
    override val neptune = TransformedCelestialDynModel(transform, delegate.neptune)

    override fun copy() = SolarSystemTransformedDynModel(transform, delegate)
}

fun SolarSystemDynModel.transformed(transform: TransformedCelestialDynModel.Transform) =
    SolarSystemTransformedDynModel(transform, this)