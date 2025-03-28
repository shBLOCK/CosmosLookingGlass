package ui

import de.fabmax.kool.KoolContext
import de.fabmax.kool.input.InputStack
import de.fabmax.kool.input.Pointer
import de.fabmax.kool.input.PointerState
import de.fabmax.kool.math.*
import de.fabmax.kool.pipeline.RenderPass
import de.fabmax.kool.scene.Node
import de.fabmax.kool.scene.OrthographicCamera
import de.fabmax.kool.scene.PerspectiveCamera
import de.fabmax.kool.util.BaseReleasable
import de.fabmax.kool.util.Time
import de.fabmax.kool.util.Viewport
import utils.atan2
import utils.expDecaySnapping
import utils.rotate
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.tan

class MainCameraControl(val view: RenderPass.View) : BaseReleasable(), InputStack.PointerListener {
    companion object {
        private val DEFAULT_HALF_FOV = 35.0.deg
        private const val MIN_HALF_FOV_RAD = 0.02

        private const val SCROLL_ZOOM_SPEED = 0.1
        private const val DRAG_ROTATE_SPEED = 0.15

        private const val MAX_FAR_TO_NEAR_RATIO = 3e5
    }

    init {
        InputStack.defaultInputHandler.pointerListeners += this
        onRelease { InputStack.defaultInputHandler.pointerListeners -= this }
    }

    val viewport by view::viewport
    var camera by view::camera
        private set

    val targetParams = Params()
    val params = Params()

    private val pointers = mutableListOf<Pointer>()
    private var lastGesturePtrPair: Pair<Vec2d, Vec2d>? = null

    override fun handlePointer(pointerState: PointerState, ctx: KoolContext) {
        pointerState.getActivePointers(pointers)

        // single pointer
        if (pointers.size == 1) {
            val pointer = pointers[0]

            with(targetParams) {
                if (pointer.isRightButtonDown) {
                    val translation = pointer.delta.toMutableVec2d().apply {
                        divAssign(viewport.size.y.toDouble())
                        mul(2.0)
                        y = -y
                    }
                    center.subtract(sdrOnMainPlaneLocal(translation))
                }

                if (pointer.scroll.y != 0F) {
                    val zoom = pointer.scroll.y.toDouble() * -SCROLL_ZOOM_SPEED
                    center.subtract(sdrOnMainPlaneLocal(pointer.sdr) * zoom)
                    halfSize *= 1.0 + zoom
                }

                if (pointer.isLeftButtonDown) {
                    val rot = pointer.delta.toMutableVec2d()
                        .mul(pointer.windowScale.toDouble() * DRAG_ROTATE_SPEED)
                        .apply { y = -y }
                    orientation = orientation.rotateByEulers(Vec3d(rot.y, -rot.x, 0.0), EulerOrder.ZYX)
                }

                if (pointer.isMiddleButtonClicked) {
                    halfFov = if (halfFov.rad == 0.0) 35.0.deg else 0.0.rad
                }
            }
        }

        // double pointer gesture
        if (pointers.size == 2) {
            if (lastGesturePtrPair == null) snapToCurrent()

            val current = pointers[0].sdr to pointers[1].sdr

            with(targetParams) {
                lastGesturePtrPair?.also { last ->
                    val lastCenter = (last.first + last.second) / 2.0
                    val currentCenter = (current.first + current.second) / 2.0
                    val lastDist = last.first.distance(last.second)
                    val currentDist = current.first.distance(current.second)
                    val lastAngle = atan2(last.second - last.first)
                    val currentAngle = atan2(current.second - current.first)

                    val translation = currentCenter - lastCenter
                    if (!translation.isFuzzyEqual(Vec2d.ZERO)) {
                        center.subtract(sdrOnMainPlaneLocal(translation))
                    }

                    val zoom = 1.0 - currentDist / lastDist
                    if (!zoom.isFuzzyZero()) {
                        center.subtract(sdrOnMainPlaneLocal(currentCenter) * zoom)
                        halfSize *= 1.0 + zoom
                    }

                    val rot = (currentAngle - lastAngle).wrap(-PI, PI)
                    val axis = sdrRayLocal(currentCenter)
                        .also { it.origin.add(center) }
                        .also { it.direction.norm() }
                    orientation = orientation.rotate(rot.rad, axis.direction)
                    center.rotate(rot.rad, axis)
                }
            }

            lastGesturePtrPair = current
        } else {
            lastGesturePtrPair = null
        }
    }

    fun snapToCurrent() = targetParams.set(params)
    fun snapToTarget() = params.set(targetParams)

    fun update() {
        targetParams.nearClipDist = targetParams.halfSize / tan(max(targetParams.halfFov.rad, DEFAULT_HALF_FOV.rad)) - 1

        with(params) {
            // smooth motion
            halfFov = halfFov.rad.expDecaySnapping(targetParams.halfFov.rad, 12.0).rad
            halfSize = halfSize.expDecaySnapping(targetParams.halfSize, 24.0)
            nearClipDist = nearClipDist.expDecaySnapping(targetParams.nearClipDist, 24.0)
            center.expDecaySnapping(targetParams.center, 24.0)
            orientation = orientation
                .mix(targetParams.orientation, exp(-8.0 * Time.deltaT), result = orientation)
                .norm()
            farClipDist = targetParams.farClipDist

            if (halfFov.rad > MIN_HALF_FOV_RAD) {
                var d = halfSize / halfFov.tan
                if (true || !camera.isZeroToOneDepth) {
                    val nearClip = d - nearClipDist
                    val farClip = d + farClipDist
                    // avoid too small near clip values to avoid depth precision artifacts in legacy depth mode
                    if (farClip / nearClip > MAX_FAR_TO_NEAR_RATIO) {
                        nearClipDist = d - farClip / MAX_FAR_TO_NEAR_RATIO
                    }
                }
            }
        }

        params.apply()
    }

    inline val Viewport.pos get() = Vec2i(x, y)
    inline val Viewport.size get() = Vec2i(width, height)
    fun Viewport.ndr(scrPos: Vec2d) = MutableVec2d(scrPos).apply {
        subtract(pos.toVec2d())
        divAssign(size.toVec2d())
        mul(2.0)
        subtract(1.0)
        y = -y
    }

    /** SDR ("Standard device coordinates"): like NDR, but only normalized on the y direction (aspect ratio dependent) */
    fun Viewport.sdr(scrPos: Vec2d) = ndr(scrPos).run { Vec2d(x * aspectRatio, y) }

    val Pointer.sdr get() = viewport.sdr(pos.toVec2d())

    inner class Params {
        /** Single side fov (half of normal fov) */
        var halfFov: AngleD = 35.0.deg

        /** Center of the camera control. The camera will always "look at" this position. */
        val center = MutableVec3d()

        /** Half vertical size of the "main plane" (the plane in the camera frustum that is perpendicular to [dir] and passes through [center]) */
        var halfSize: Double = 8e10

        /** Distance between [center] and near clip plane */
        var nearClipDist = halfSize / DEFAULT_HALF_FOV.tan - 1.0

        /** Distance between [center] and far clip plane */
        var farClipDist = 1e13

        var orientation = MutableQuatD()
            set(value) {
                field = value
                orientationMat.setIdentity().rotate(value)
            }

        fun set(target: Params) {
            halfFov = target.halfFov
            nearClipDist = target.nearClipDist
            farClipDist = target.farClipDist
            center.set(target.center)
            halfSize = target.halfSize
            orientation = orientation.set(target.orientation)
        }

        var orientationMat = MutableMat3d()
            set(value) {
                field = value
                value.getRotation(orientation)
            }
        inline var dir: Vec3d
            get() = orientationMat[2] * -1.0
            set(value) {
                orientationMat = orientationMat.also { it[2] = value * -1.0 }
            }
        inline var up
            get() = orientationMat[1]
            set(value) {
                orientationMat = orientationMat.also { it[1] = value }
            }
        inline var right
            get() = orientationMat[0]
            set(value) {
                orientationMat = orientationMat.also { it[0] = value }
            }

        fun apply() {
            if (halfFov.rad <= MIN_HALF_FOV_RAD) {
                if (camera !is OrthographicCamera) camera = OrthographicCamera()
            } else {
                if (camera !is PerspectiveCamera) camera = PerspectiveCamera()
            }

            camera.up.set(up)
            camera.lookAt.set(center + dir)

            camera.also { cam ->
                when (cam) {
                    is OrthographicCamera -> {
                        cam.top = halfSize.toFloat()
                        cam.bottom = -halfSize.toFloat()
                        cam.clipNear = 0F
                        cam.clipFar = (nearClipDist + farClipDist).toFloat()
                        cam.position.set(center + dir * -nearClipDist)
                    }

                    is PerspectiveCamera -> {
                        cam.fovY = (halfFov.rad * 2.0).toFloat().rad
                        val d = halfSize / halfFov.tan
                        cam.position.set(center + dir * -d)
                        cam.clipNear = (d - nearClipDist).toFloat()
                        cam.clipFar = (d + farClipDist).toFloat()
                    }
                }
            }
        }

        /**
         * Ray from the near plane to the far plane. Actual length, not normalized. Not translated by [center].
         */
        fun sdrRayLocal(sdr: Vec2d): RayD {
            if (halfFov.rad > MIN_HALF_FOV_RAD) {
                val d = halfSize / halfFov.tan
                val near = d - nearClipDist
                val far = d + farClipDist
                val point = sdrOnMainPlaneLocal(sdr)
                val nearPoint = point * (near / d) - dir * nearClipDist
                val farPoint = point * (far / d) + dir * farClipDist
                return RayD(nearPoint, farPoint - nearPoint)
            } else {
                val point = sdrOnMainPlaneLocal(sdr)
                return RayD(point - dir * nearClipDist, dir * (nearClipDist + farClipDist))
            }
        }

        fun sdrOnMainPlaneLocal(sdr: Vec2d) = right * (sdr.x * halfSize) + up * (sdr.y * halfSize)
    }
}