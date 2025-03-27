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
import de.fabmax.kool.util.Time
import de.fabmax.kool.util.Viewport
import utils.expDecaySnapping
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.tan

class MainCameraControl(val view: RenderPass.View) : Node(), InputStack.PointerListener {
    companion object {
        private val DEFAULT_HALF_FOV = 35.0.deg
        private const val MIN_HALF_FOV_RAD = 0.03

        private const val SCROLL_ZOOM_SPEED = 0.1
        private const val DRAG_ROTATE_SPEED = 0.15

        private const val MAX_FAR_TO_NEAR_RATIO = 1e7
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

    private var scrollZoom = 0.0
    private var scrollZoomCenter = Vec2d.ZERO

    private var dragRotate = Vec2d.ZERO

    private var dragTranslate = Vec2d.ZERO

    private val pointers = mutableListOf<Pointer>()

    override fun handlePointer(pointerState: PointerState, ctx: KoolContext) {
        pointerState.getActivePointers(pointers)

        if (pointers.size == 1) {
            val pointer = pointers[0]
            if (pointer.scroll.y != 0F) {
                scrollZoom = pointer.scroll.y.toDouble() * -SCROLL_ZOOM_SPEED
                scrollZoomCenter = viewport.sdr(pointer.pos.toVec2d())
            }
            if (pointer.isLeftButtonDown) {
                dragRotate = pointer.delta.toMutableVec2d()
                    .mul(pointer.windowScale.toDouble() * DRAG_ROTATE_SPEED)
                    .apply { y = -y }
            }
            if (pointer.isRightButtonDown) {
                dragTranslate = pointer.delta.toMutableVec2d().apply {
                    divAssign(viewport.size.y.toDouble())
                    mul(2.0)
                    y = -y
                }
            }
            if (pointer.isMiddleButtonClicked) {
                targetParams.halfFov = if (targetParams.halfFov.rad == 0.0) 35.0.deg else 0.0.rad
            }
        } else if (pointers.size > 1) {

        }
    }

    fun snapToCurrent() = targetParams.set(params)
    fun snapToTarget() = params.set(targetParams)

    override fun update(updateEvent: RenderPass.UpdateEvent) {
        if (scrollZoom != 0.0) {
            targetParams.center.subtract(targetParams.sdrOnMainPlane(scrollZoomCenter) * scrollZoom)
            targetParams.halfSize *= 1.0 + scrollZoom
            scrollZoom = 0.0
        }

        if (dragTranslate != Vec2d.ZERO) {
            targetParams.center.subtract(targetParams.sdrOnMainPlane(dragTranslate))
            dragTranslate = Vec2d.ZERO
        }

        if (dragRotate != Vec2d.ZERO) {
            targetParams.orientation = targetParams.orientation
                .rotateByEulers(Vec3d(dragRotate.y, -dragRotate.x, 0.0), EulerOrder.ZYX)
            dragRotate = Vec2d.ZERO
        }

        targetParams.nearClipDist = targetParams.halfSize / tan(max(targetParams.halfFov.rad, DEFAULT_HALF_FOV.rad)) - 1

        // Smooth motion
        params.halfFov = params.halfFov.rad.expDecaySnapping(targetParams.halfFov.rad, 12.0).rad
        params.halfSize = params.halfSize.expDecaySnapping(targetParams.halfSize, 24.0)
        params.nearClipDist = params.nearClipDist.expDecaySnapping(targetParams.nearClipDist, 24.0)
        params.center.expDecaySnapping(targetParams.center, 24.0)
        params.orientation =
            params.orientation
                .mix(targetParams.orientation, exp(-8.0 * Time.deltaT), result = params.orientation)
                .norm()
        params.absFarClip = targetParams.absFarClip

        with(params) {
            var dirDist = halfSize / halfFov.tan
            if (!camera.isZeroToOneDepth) {
                val nearClip = dirDist - nearClipDist
                // avoid too small near clip values to avoid depth precision artifacts in non-reverse depth mode
                if (absFarClip / nearClip > MAX_FAR_TO_NEAR_RATIO) {
                    nearClipDist = dirDist - absFarClip / MAX_FAR_TO_NEAR_RATIO
                }
            }
        }

        params.apply()

        super.update(updateEvent)
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

    inner class Params {
        /** Single side fov (half of normal fov) */
        var halfFov: AngleD = 35.0.deg

        /** Center of the camera control. The camera will always "look at" this position. */
        val center = MutableVec3d()

        /** Half vertical size of the "main plane" (the plane in the camera frustum that is perpendicular to [dir] and passes through [center]) */
        var halfSize: Double = 8e10

        /** Distance between [center] and near clip plane */
        var nearClipDist = halfSize / DEFAULT_HALF_FOV.tan - 1.0

        /** Absolute far clip plane distance */
        var absFarClip = 1e15

        var orientation = MutableQuatD()
            set(value) {
                field = value
                orientationMat.setIdentity().rotate(value)
            }

        fun set(target: Params) {
            halfFov = target.halfFov
            nearClipDist = target.nearClipDist
            absFarClip = target.absFarClip
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
            camera.clipFar = absFarClip.toFloat()

            camera.also { cam ->
                when (cam) {
                    is OrthographicCamera -> {
                        cam.top = halfSize.toFloat()
                        cam.bottom = -halfSize.toFloat()
                        cam.clipNear = 0F
                        cam.position.set(center + dir * -nearClipDist)
                    }

                    is PerspectiveCamera -> {
                        cam.fovY = (halfFov.rad * 2.0).toFloat().rad
                        val d = halfSize / halfFov.tan
                        cam.position.set(center + dir * -d)
                        cam.clipNear = (d - nearClipDist).toFloat()
                    }
                }
            }
        }

        fun sdrDir(sdr: Vec2d) =
            if (halfFov.rad > MIN_HALF_FOV_RAD) {
                orientationMat.transform(MutableVec3d(sdr.x, sdr.y, 1.0 / halfFov.tan))
            } else dir

        fun sdrOnMainPlane(sdr: Vec2d) = right * (sdr.x * halfSize) + up * (sdr.y * halfSize)
    }
}