package dynamics

import de.fabmax.kool.math.MutableQuatD
import de.fabmax.kool.math.MutableVec3d
import universe.CelestialBody
import utils.IntFract
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

abstract class LazyDynModelBase : DynModelBase() {
    private var lastTime: IntFract? = null

    @PublishedApi
    internal var dirty = true

    override fun seek(time: IntFract) {
        super.seek(time)
        if (time != lastTime) {
            dirty = true
            lastTime = time
        }
    }

    fun markDirty() {
        dirty = true
    }

    @OptIn(ExperimentalContracts::class)
    protected inline fun lazyPath(block: () -> Unit) {
        contract { callsInPlace(block, InvocationKind.AT_MOST_ONCE) }
        if (!dirty) {
            block()
            dirty = false
        }
    }
}

class TransformedDynModel(
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
            transform(time, _position, _orientation)
            dirty = false
        }
    }

    override fun position(result: MutableVec3d) = update().let { result.set(_position) }
    override fun orientation(result: MutableQuatD) = update().let { result.set(_orientation) }

    override fun copy() = TransformedDynModel(transform, delegate.copyTyped())

    typealias Transform = (time: IntFract, position: MutableVec3d, orientation: MutableQuatD) -> Unit
}

fun CelestialDynModel.transformed(transform: TransformedDynModel.Transform) =
    TransformedDynModel(transform, this)


class TransformedUniverseDynModel(
    private val transform: TransformedDynModel.Transform,
    private val delegate: UniverseDynModelImpl
) : UniverseDynModelImpl() {
    override fun getDynModelFor(celestialBody: CelestialBody): CelestialDynModel? =
        delegate.getDynModelFor(celestialBody)?.transformed(transform)

    override fun seek(time: IntFract) {
        delegate.seek(time)
        super.seek(time)
    }
}

fun UniverseDynModelImpl.transformed(transform: TransformedDynModel.Transform) =
    TransformedUniverseDynModel(transform, this)


class RelativePositionModel(
    private val reference: DynModel.Position,
    private val delegate: DynModel.Position
) : DynModel.Position by delegate {
    override fun position(result: MutableVec3d): MutableVec3d {
        delegate.position(result)
        reference.seek(time)
        result += reference.position()
        return result
    }

    override fun copy() = RelativePositionModel(reference.copyTyped(), delegate.copyTyped())
}

fun DynModel.Position.relativeTo(reference: DynModel.Position) = RelativePositionModel(reference, this)