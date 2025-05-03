package ui

import de.fabmax.kool.input.Pointer
import de.fabmax.kool.input.PointerInput
import de.fabmax.kool.input.PointerState
import de.fabmax.kool.math.*
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.pipeline.RenderPass
import de.fabmax.kool.scene.OrthographicCamera
import de.fabmax.kool.scene.PerspectiveCamera
import de.fabmax.kool.util.BaseReleasable
import de.fabmax.kool.util.Time
import de.fabmax.kool.util.Viewport
import de.fabmax.kool.util.logW
import ui.hud.HudViewport
import utils.*
import kotlin.math.*

class MainCameraControl(val view: RenderPass.View) : BaseReleasable() {
    companion object {
        private val DEFAULT_HALF_FOV = 35.0.deg
        private const val MIN_HALF_FOV_RAD = 0.02

        private const val SCROLL_ZOOM_SPEED = 0.1
        private const val DRAG_ROTATE_SPEED = 0.15

        private const val MAX_FAR_TO_NEAR_RATIO = 1e7
    }

    val viewport by view::viewport
    var camera by view::camera
        private set

    val targetParams = Params()
    val params = Params()

    val trackingFocusPos = MutableVec3d()
    val trackingFocusRot = MutableQuatD()
    private val lastTrackingFocusRot = MutableQuatD()
    var trackingFocusFollowRot = false
        set(value) {
            field = value
            startFollowRotAnimation.start(value.toFloat())
        }
    private val startFollowRotAnimation = AnimatedFloatBidir(1F)
    var startFollowRotAnimationDuration
        get() = startFollowRotAnimation.fwdDuration
        set(value) {
            startFollowRotAnimation.fwdDuration = value
            startFollowRotAnimation.bwdDuration = value
        }

    // cfta: change tracking focus animation
    // use bidir versions which can be reset manually
    private var ctfaLookAtAnimation = AnimatedFloat(1F, initValue = 1F)
    private var ctfaZoomAnimation = AnimatedFloat(1F, initValue = 1F)
    private val ctfaInitialParams = Params()
    private var ctfaFinalHalfSize = 0.0

    fun startChangeTrackingFocusAnimation(
        halfSize: Double,
        durationLookAt: Double = 1.0,
        durationZoom: Double = 2.0
    ) {
        ctfaFinalHalfSize = halfSize
        ctfaLookAtAnimation = AnimatedFloat(durationLookAt.toFloat(), 0F).apply { start() }
        ctfaZoomAnimation = AnimatedFloat(durationZoom.toFloat(), 0F)
        startFollowRotAnimation.start(0F)
        startFollowRotAnimation.value = 0F

        lastTrackingFocusRot.set(trackingFocusRot)

        snapToCurrent()
        ctfaInitialParams.set(params)
        // zero out local pos & rot
        ctfaInitialParams.centerOffset += ctfaInitialParams.localCenter
        ctfaInitialParams.localCenter = Vec3d.ZERO
        ctfaInitialParams.orientationOffset *= ctfaInitialParams.localOrientation
        ctfaInitialParams.localOrientation = QuatD.IDENTITY
    }

    private fun updateTrackingFocus() {
        if (ctfaLookAtAnimation.isActive || ctfaZoomAnimation.isActive) {
            ctfaLookAtAnimation.progress(Time.deltaT)

            val tmpVec3d = MutableVec3d()

            // calculate final camera orientation
            val camPos = ctfaInitialParams.centerOffset - ctfaInitialParams.dir * ctfaInitialParams.camDist
            val finalCamLook = trackingFocusPos - camPos
            val oriMat = MutableMat3d().rotate(ctfaInitialParams.orientationOffset)
            oriMat[2] = tmpVec3d.set(finalCamLook).norm().mul(-1.0)
            oriMat[0] = oriMat[1].cross(oriMat[2], tmpVec3d)
            oriMat[1] = oriMat[2].cross(oriMat[0], tmpVec3d)
            val finalOrientation = oriMat.getRotation()

            // interpolate orientation
            val tLookAt = smoothStep(0F, 1F, ctfaLookAtAnimation.value).toDouble()
            val oriQuat =
                ctfaInitialParams.orientationOffset.mix(finalOrientation, tLookAt)
            oriMat.setIdentity().rotate(oriQuat)
            val dir = oriMat[2] * -1.0

            // start zoom animation when cam sweep is almost done
            if (ctfaZoomAnimation.value == 0F) {
                if ((dir dot finalCamLook) / finalCamLook.length() > 0.99)
                    ctfaZoomAnimation.start()
            }
            ctfaZoomAnimation.progress(Time.deltaT)

            // calculate center: find the point on the camera sweep path that intersects with camera look ray
            val center = rayLineSegmentIntersectionUnchecked(
                RayD(camPos, dir),
                ctfaInitialParams.centerOffset,
                trackingFocusPos
            )

            // zoom
            val tZoom = smoothStep(0F, 1F, ctfaZoomAnimation.value).toDouble()
            // the raw half-size is the half-size required to make camera position not change during sweep
            val rawHalfSizeExp = ln(ctfaInitialParams.halfSize * (center.distance(camPos) / ctfaInitialParams.camDist))
            val halfSize = exp(mix(rawHalfSizeExp, ln(ctfaFinalHalfSize), tZoom))

            // apply
            targetParams.set(ctfaInitialParams)
            targetParams.orientationOffset = oriQuat
            targetParams.centerOffset = center
            targetParams.halfSize = halfSize

            // trigger ctfaStartFollowRotAnimation when zoom is almost done
            if (trackingFocusFollowRot && startFollowRotAnimation.value == 0F) {
                if (ctfaZoomAnimation.value > 0.9)
                    startFollowRotAnimation.start(1F)
            }

            snapToTarget()
        } else {
            targetParams.centerOffset = Vec3d(trackingFocusPos)
        }

        val deltaRot = MutableQuatD(trackingFocusRot).mul(lastTrackingFocusRot.invert())
        lastTrackingFocusRot.set(trackingFocusRot)
        startFollowRotAnimation.progress(Time.deltaT)
        targetParams.orientationOffset *=
            if (startFollowRotAnimation.value < 1F)
                QuatD.IDENTITY.mix(deltaRot, startFollowRotAnimation.value.toDouble())
            else deltaRot
    }

    private val pointers = mutableListOf<Pointer>()
    private var lastGesturePtrPair: Pair<Vec2d, Vec2d>? = null

    private fun handlePointerScroll(event: PointerEvent) {
        val pointer = event.pointer
        with(targetParams) {
            if (pointer.scroll.y != 0F) {
                val zoom = pointer.scroll.y.toDouble() * -SCROLL_ZOOM_SPEED
                center -= sdrOnMainPlaneLocal(pointer.sdr) * zoom
                halfSize *= 1.0 + zoom
            }
        }
    }

    private fun handlePointer(pointerState: PointerState = PointerInput.pointerState) {
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
                    center -= sdrOnMainPlaneLocal(translation)
                }

                if (pointer.isLeftButtonDown) {
                    val rot = pointer.delta.toMutableVec2d()
                        .mul(pointer.windowScale.toDouble() * DRAG_ROTATE_SPEED)
                        .apply { y = -y }
                    orientation = MutableQuatD(orientation).rotateByEulers(Vec3d(rot.y, -rot.x, 0.0), EulerOrder.ZYX)
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
                        center -= sdrOnMainPlaneLocal(translation)
                    }

                    val zoom = 1.0 - currentDist / lastDist
                    if (!zoom.isFuzzyZero()) {
                        center -= sdrOnMainPlaneLocal(currentCenter) * zoom
                        halfSize *= 1.0 + zoom
                    }

                    val rot = (currentAngle - lastAngle).wrap(-PI, PI)
                    val axis = sdrRayLocal(currentCenter)
                        .also { it.origin.add(center) }
                        .also { it.direction.norm() }
                    orientation = MutableQuatD(orientation).rotate(rot.rad, axis.direction)
                    center = MutableVec3d(center).rotate(rot.rad, axis)
                }
            }

            lastGesturePtrPair = current
        } else {
            lastGesturePtrPair = null
        }
    }

    fun attachTo(viewport: HudViewport) = viewport.apply {
        if (!isFuzzyEqual(rect, view.viewport.rect))
            this@MainCameraControl.logW { "Actual viewport (${view.viewport.rect}) doesn't match HudViewport ($rect), expect weird behaviors." }
        modifier.onDrag { handlePointer() }
        modifier.onClick { handlePointer() }
        modifier.onWheelY += ::handlePointerScroll
    }

    fun snapToCurrent() = targetParams.set(params)
    fun snapToTarget() = params.set(targetParams)

    fun update() {
        updateTrackingFocus()

        with(params) {
            // smooth motion
            halfFov = halfFov.rad.expDecaySnapping(targetParams.halfFov.rad, 12.0).rad
            halfSize = halfSize.expDecaySnapping(targetParams.halfSize, 24.0)
            farClipDist = targetParams.farClipDist
            centerOffset = targetParams.centerOffset // no smoothing
            localCenter = MutableVec3d(localCenter).expDecaySnapping(targetParams.localCenter, 24.0)
            orientationOffset = targetParams.orientationOffset // no smoothing
            localOrientation = MutableQuatD(localOrientation).expDecaySnapping(targetParams.localOrientation, 8.0)
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

        /** Half vertical size of the "main plane" (the plane in the camera frustum that is perpendicular to [dir] and passes through [center]) */
        var halfSize: Double = 8e10

        /** Distance between [center] and the camera position (or the near plane for small fov) */
        val camDist get() = halfSize / tan(max(halfFov.rad, DEFAULT_HALF_FOV.rad))

        /** Distance between [center] and far clip plane */
        var farClipDist = 1e13

        var centerOffset = Vec3d.ZERO
            set(value) {
                field = value
                _center.set(value).add(localCenter)
            }
        var localCenter = Vec3d.ZERO
            set(value) {
                field = value
                _center.set(centerOffset).add(value)
            }

        private val _center = MutableVec3d()

        /** Center of the camera control. The camera will always "look at" this position. */
        var center: Vec3d
            get() = _center
            set(value) {
                localCenter = value - centerOffset
            }

        var orientationOffset = QuatD.IDENTITY
            set(value) {
                field = value
                _orientation.set(value).mul(localOrientation)
            }
        var localOrientation = QuatD.IDENTITY
            set(value) {
                field = value
                _orientation.set(orientationOffset).mul(value)
            }

        private val _orientation = MutableQuatD()
        var orientation: QuatD
            get() = _orientation
            set(value) {
                localOrientation = orientationOffset.inverted().mul(value)
            }

        fun set(target: Params) {
            halfFov = target.halfFov
            farClipDist = target.farClipDist
            halfSize = target.halfSize
            centerOffset = target.centerOffset
            localCenter = target.localCenter
            orientationOffset = target.orientationOffset
            localOrientation = target.localOrientation
        }

        val orientationMat: Mat3d get() = MutableMat3d().rotate(orientation)
        inline val dir get() = orientationMat[2] * -1.0
        inline val up get() = orientationMat[1]
        inline val right get() = orientationMat[0]

        fun apply() {
            if (halfFov.rad <= MIN_HALF_FOV_RAD) {
                if (camera !is OrthographicCamera) {
                    camera.parent?.removeNode(camera)
                    camera.release()
                    camera = OrthographicCamera()
                }
            } else {
                if (camera !is PerspectiveCamera) {
                    camera.parent?.removeNode(camera)
                    camera.release()
                    camera = PerspectiveCamera()
                }
            }

            camera.up.set(up)
            camera.lookAt.set(center + dir)

            camera.also { cam ->
                when (cam) {
                    is OrthographicCamera -> {
                        cam.top = halfSize.toFloat()
                        cam.bottom = -halfSize.toFloat()
                        cam.clipNear = 0F
                        cam.clipFar = (camDist + farClipDist).toFloat()
                        cam.position.set(center + dir * -camDist)
                    }

                    is PerspectiveCamera -> {
                        cam.fovY = (halfFov.rad * 2.0).toFloat().rad
                        val d = halfSize / halfFov.tan
                        cam.position.set(center + dir * -d)
                        cam.clipNear = max(d - camDist, 1.0).toFloat()
                        cam.clipFar = (d + farClipDist).toFloat()

                        if (true || !camera.isZeroToOneDepth) {
                            // avoid too small near clip values to avoid depth precision artifacts in legacy depth mode
                            if (cam.clipFar / cam.clipNear > MAX_FAR_TO_NEAR_RATIO) {
                                cam.clipNear = (cam.clipFar / MAX_FAR_TO_NEAR_RATIO).toFloat()
                            }
                        }
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
                val near = max(d - camDist, 0.0)
                val far = d + farClipDist
                val point = sdrOnMainPlaneLocal(sdr)
                val nearPoint = point * (near / d) - dir * camDist
                val farPoint = point * (far / d) + dir * farClipDist
                return RayD(nearPoint, farPoint - nearPoint)
            } else {
                val point = sdrOnMainPlaneLocal(sdr)
                return RayD(point - dir * camDist, dir * (camDist + farClipDist))
            }
        }

        fun sdrOnMainPlaneLocal(sdr: Vec2d) = right * (sdr.x * halfSize) + up * (sdr.y * halfSize)
    }
}