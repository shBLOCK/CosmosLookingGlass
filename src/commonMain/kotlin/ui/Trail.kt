@file:Suppress("LocalVariableName")

package ui

import de.fabmax.kool.KoolSystem
import de.fabmax.kool.math.*
import de.fabmax.kool.math.spatial.BoundingBoxF
import de.fabmax.kool.modules.ksl.KslShader
import de.fabmax.kool.modules.ksl.blocks.cameraData
import de.fabmax.kool.modules.ksl.blocks.mvpMatrix
import de.fabmax.kool.modules.ksl.lang.*
import de.fabmax.kool.pipeline.*
import de.fabmax.kool.scene.Mesh
import de.fabmax.kool.scene.geometry.IndexedVertexList
import de.fabmax.kool.scene.geometry.PrimitiveType
import de.fabmax.kool.util.*
import dynamics.CelestialDynModel
import dynamics.copyTyped
import platform.ex
import universe.CelestialBody
import utils.*
import kotlin.math.max
import kotlin.math.min

class Trail(val celestialBody: CelestialBody) : BaseReleasable() {
    var stepSize = 3600 * 24
        set(value) {
            field = value
            dataTex.clear()
        }
    var startTime = IntFract(0L)
    var endTime = IntFract(-stepSize.toLong())
    var currentTime = IntFract(0L)
    private val duration get() = endTime - startTime
    private val startStep get() = startTime.floorDiv(stepSize)
    private val endStep get() = endTime.ceilDiv(stepSize)
    private val startFractStep get() = startTime.mod(stepSize) / stepSize
    private val durationFractStep get() = duration / stepSize

    private val dataTex = DataTex()
    val meshInstances = mutableListOf(MeshInstance())

    fun update() {
        dataTex.update()
    }

    /**
     * Data texture to store trail position data.
     *
     * Acts sort of like a ring buffer, but since data is strictly chronological and continuous,
     * the position of data in the buffer is simply always `(time step) mod (buffer size)`.
     *
     * P.S.: To workaround texture size limitations, we use 2d textures.
     * Thus, to ensure correct interpolation between rows,
     * the pixels at the left edge of a row and right edge of the next row of the texture represents the same step.
     */
    inner class DataTex : BaseReleasable() {
        init {
            releaseWith(this@Trail)
        }

        private val baseName = "Trail.DataTex(${celestialBody.name})"

        private val texSize = MutableVec2i(1, 1)
        private inline val size get() = texSize.x * texSize.y
        private var buffer = Float32Buffer(texSize.x * texSize.y * 4)
        val texture = StorageTexture2d(
            texSize.x, texSize.y, TexFormat.RGBA_F32, MipMapping.Off,
            SamplerSettings().repeating().linear(),
            name = UniqueId.nextId(baseName)
        ).releaseWith(this)

        var batchSize = 100
        private inline val trail get() = this@Trail
        private var headStep: Long = 0L
        private var tailStep: Long = -1L
        private val headPos get() = headStep.mod(size)
        private val tailPos get() = tailStep.mod(size)

        private fun Float32Buffer.getData(step: Long, result: MutableVec4f, texSize: Vec2i = this@DataTex.texSize) =
            getVec4f(step.mod(texSize.x * texSize.y), result)

        private fun Float32Buffer.setData(step: Long, data: Vec4f, texSize: Vec2i = this@DataTex.texSize) =
            setVec4f(step.mod(texSize.x * texSize.y), data)

        fun clear() {
            headStep = 0L
            tailStep = -1L
        }

        private fun expand(minSize: Int, newHeadStep: Long = headStep) {
            val maxWidth = KoolSystem.requireContext().backend.ex.maxTextureSize.nextPowerOfTwo
            val newTexSize = Vec2i(
                min(minSize.nextPowerOfTwo, maxWidth),
                minSize.ceilDiv(maxWidth)
            )
            val newSize = newTexSize.x * newTexSize.y

            val newTailStep = min(newHeadStep + newSize, tailStep)

            buffer = Float32Buffer(newTexSize.x * newTexSize.y * 4).apply {
                val tmpVec4f = MutableVec4f()
                for (step in newHeadStep..newTailStep) {
                    setData(step, texSize = newTexSize, data = buffer.getData(step, result = tmpVec4f))
                }
            }

            headStep = newHeadStep
            tailStep = newTailStep

            texSize.set(newTexSize).also {
                texture.resize(it.x, it.y)
                texture.uploadLazy(
                    BufferedImageData2d(
                        buffer, it.x, it.y, TexFormat.RGBA_F32,
                        id = UniqueId.nextId("$baseName upload buffer (resize)")
                    )
                )
            }
        }

        private fun CelestialDynModel.generate(range: LongRange) {
            val tmpVec3d = MutableVec3d()
            val tmpVec4f = MutableVec4f()
            for (step in range) {
                seek(IntFract(step * stepSize))
                position(result = tmpVec3d)
                buffer.setData(step, tmpVec4f.set(tmpVec3d.x.toFloat(), tmpVec3d.y.toFloat(), tmpVec3d.z.toFloat(), 0F))
            }
        }

        fun update() {
            if (headStep <= startStep && tailStep >= endStep) return

            // use 2x batchSize so we can batch in both directions
            val minSize = (endStep - startStep + 1).toInt() + (batchSize * 2)
            if (size < minSize) expand(minSize)

            val dyn = celestialBody.dynModel?.copyTyped() ?: CelestialDynModel.Dummy()
            if (endStep > tailStep) {
                tailStep = (endStep + (batchSize - 1)).also {
                    dyn.generate(max(tailStep + 1, startStep)..it)
                    headStep = max(headStep, it - (size - 1))
                    if (startStep > tailStep + 1) headStep = startStep
                }
            }
            if (startStep < headStep) {
                headStep = (startStep - (batchSize - 1)).also {
                    dyn.generate(it..min(headStep - 1, endStep))
                    tailStep = min(tailStep, it + (size - 1))
                    if (endStep < headStep - 1) tailStep = endStep
                }
            }

            texture.uploadLazy(
                BufferedImageData2d(
                    buffer, texSize.x, texSize.y, TexFormat.RGBA_F32,
                    id = UniqueId.nextId("$baseName upload buffer (update)")
                )
            )
        }

        fun updateInfoStruct(struct: DataTextureInfo, relativeTo: DataTex = this) = struct.run {
            val relStartSec = relativeTo.trail.startStep * relativeTo.trail.stepSize
            val relEndSec = relativeTo.trail.endStep * relativeTo.trail.stepSize
            val headSec = headStep * stepSize
            val tailSec = tailStep * stepSize
            headOffset.set((IntFract(headSec - relStartSec) / stepSize).toFloat())
            tailOffset.set((IntFract(tailSec - relEndSec) / stepSize).toFloat())

            headPos.set(this@DataTex.headPos)
            tailPos.set(this@DataTex.tailPos)

            relStepSize.set((stepSize.toDouble() / relativeTo.trail.stepSize.toDouble()).toFloat())
        }
    }

    class DataTextureInfo : Struct("DataTextureInfo", MemoryLayout.Std140) {
        /** headStep - startStep */
        val headOffset = float1("headOffset")

        /** tailStep - endStep */
        val tailOffset = float1("tailOffset")

        /** Pixel coord of the head in the data texture */
        val headPos = int1("headPos")

        /** Pixel coord of the tail in the data texture */
        val tailPos = int1("tailPos")

        /** Step size of the relative texture, in terms of this texture's step */
        val relStepSize = float1("relStepSize")
    }

    inner class MeshInstance : Mesh(
        IndexedVertexList(listOf(), PrimitiveType.TRIANGLE_STRIP),
        name = makeNodeName("Trail.Instance(${celestialBody.name})")
    ) {
        init {
            this@Trail.onRelease { parent?.removeNode(this) }
            releaseWith(this@Trail)
            isFrustumChecked = false
            isPickable = false
            isCastingShadow = false

            shader = Shader()
        }

        private val segments get() = max(geometry.numVertices / 2 - 1, 0)

        var ref: Trail? = null
        var alterRef: Trail? = null
        var refMix = 0.0
        var originMoveWithRef = true

        override fun update(updateEvent: RenderPass.UpdateEvent) {
            val minSegments = (endStep - startStep).toInt()
            if (segments < minSegments) {
                val newSegments = minSegments.nextPowerOfTwo

                with(geometry) {
                    if (newSegments < segments) {
                        val verts = if (newSegments > 0) (newSegments * 2 + 2) else 0
                        shrinkVertices(verts)
                        shrinkIndices(verts)
                    } else if (newSegments > segments) {
                        while (segments < newSegments) {
                            addIndex(addVertex { })
                            addIndex(addVertex { })
                        }
                    }
                }
            }

            val useRef = ref != null && !refMix.isNaN()

            with(shader as Shader) {
                uStart.set(startFractStep.toFloat())
                uEnd.set((startFractStep + durationFractStep).toFloat())
                uStartStep.set(0)
                uEndStep.set((endStep - startStep).toInt())

                uBaseColor.set(celestialBody.themeColor)
                uBaseLineWidth.set(3F)

                uDataTex.set(dataTex.texture)
                uDataInfo.set(dataTex::updateInfoStruct)

                if (useRef) {
                    uRefMix.set(if (alterRef == null) 0F else refMix.toFloat())
                    uRefDataTex.set(ref!!.dataTex.texture)
                    uRefDataInfo.set { ref!!.dataTex.updateInfoStruct(this, this@Trail.dataTex) }
                    if (alterRef != null) {
                        uAlterRefDataTex.set(alterRef!!.dataTex.texture)
                        uAlterRefDataInfo.set { alterRef!!.dataTex.updateInfoStruct(this, this@Trail.dataTex) }
                    }
                } else {
                    uRefMix.set(Float.NaN)
                }
            }

            if (originMoveWithRef) {
                if (useRef) {
                    val refCurrentPos = ref!!.celestialBody.dynModel?.copyTyped()?.run {
                        seek(currentTime)
                        position()
                    } ?: MutableVec3d(0.0)
                    alterRef?.celestialBody?.dynModel?.copyTyped()?.apply {
                        seek(currentTime)
                        refCurrentPos.mix(position(), refMix, result = refCurrentPos)
                    }
                    transform.setIdentity().translate(refCurrentPos)
                } else {
                    transform.setIdentity()
                }
            }

            super.update(updateEvent)
        }

        override fun addContentToBoundingBox(localBounds: BoundingBoxF) {
            // too dynamic for this to make sense
        }

        /**
         * Modified from [de.fabmax.kool.scene.TriangulatedLineMesh.Shader]
         */
        inner class Shader : KslShader("Trail Shader") {
            val uStart = uniform1f("uStart")
            val uEnd = uniform1f("uEnd")
            val uStartStep = uniform1i("uStartStep")
            val uEndStep = uniform1i("uEndStep")

            val uBaseColor = uniformColor("uBaseColor", Color.LIGHT_GRAY)
            val uBaseLineWidth = uniform1f("uBaseLineWidth", 3F)

            val uDataTex = texture2d("uDataTex")
            val uDataInfo = uniformStruct("uDataInfo") { DataTextureInfo() }
            val uRefDataTex = texture2d("uRefDataTex", DUMMY_TEXTURE)
            val uRefDataInfo = uniformStruct("uRefDataInfo") { DataTextureInfo() }
            val uAlterRefDataTex = texture2d("uAlterRefDataTex", DUMMY_TEXTURE)
            val uAlterRefDataInfo = uniformStruct("uAlterRefDataInfo") { DataTextureInfo() }

            val uRefMix = uniform1f("uRefMix", Float.NaN) // NaN represents don't use ref

            init {
                pipelineConfig = PipelineConfig(cullMethod = CullMethod.NO_CULLING)
                program.makeProgram()
            }

            @Suppress("WrapUnaryOperator")
            private fun KslProgram.makeProgram() {
                val dataTextureInfoType = struct { DataTextureInfo() }

                val uDataInfo = uniformStruct("uDataInfo") { DataTextureInfo() }
                val uRefDataInfo = uniformStruct("uRefDataInfo") { DataTextureInfo() }
                val uAlterRefDataInfo = uniformStruct("uAlterRefDataInfo") { DataTextureInfo() }

                val iStep = interStageFloat1("iStep", KslInterStageInterpolation.Smooth)

                vertexStage {
                    val cross2 = functionFloat1("cross2") {
                        val v1 = paramFloat2("v1")
                        val v2 = paramFloat2("v2")
                        body {
                            v1.x * v2.y - v1.y * v2.x
                        }
                    }

                    val rotate90 = functionFloat2("rotate90") {
                        val v = paramFloat2("v")
                        val d = paramFloat1("d")
                        body {
                            float2Value(v.y * d, -v.x * d)
                        }
                    }

                    // Note: sampleMode: 0: current; -1: previous; 1: next

                    val _fetchDataTex = functionFloat4("_fetchDataTex") {
                        val tex = paramColorTex2d("tex")
                        val pos = paramInt1("pos")
                        body {
                            val size = int2Var(tex.size(), "size")
                            val x = int1Var(mod(pos, size.x), "x")
                            val y = int1Var(pos / size.x, "y")
                            `if`((pos `{` 0.const) and (x `!=` 0.const)) { // floor div
                                y -= 1.const
                            }
                            y set mod(y, size.y)
                            tex.load(int2Value(x, y))
                        }
                    }

                    val _sampleDataTex = functionFloat4("_sampleDataTex") {
                        val tex = paramColorTex2d("tex")
                        val pos = paramFloat1("pos")
                        body {
                            val posA = int1Var(floor(pos).toInt1())
                            val posB = int1Var(ceil(pos).toInt1())
                            val a = float4Var(_fetchDataTex(tex, posA), "a")
                            `if`(posA `==` posB) { `return`(a) }
                            val b = float4Var(_fetchDataTex(tex, posB), "b")
                            mix(a, b, fract(pos))
                        }
                    }

                    val _sampleData = functionFloat3("_sampleData") {
                        val tex = paramColorTex2d("tex")
                        val info = paramStruct(dataTextureInfoType, "info")
                        val step = paramFloat1("step")
                        val sampleMode = paramInt1("sampleMode")
                        body {
                            val relStepSize = float1Var(info.struct.relStepSize.ksl)
                            val maxStepFromHead = float1Var(
                                (uEndStep.ksl.toFloat1() / relStepSize + info.struct.tailOffset.ksl)
                                    - (uStartStep.ksl.toFloat1() / relStepSize + info.struct.headOffset.ksl)
                            )

                            val step = float1Var(step, "_step")
                            `if`(sampleMode `==` -1.const) {
                                step set ceil(step) - 1F.const
                            }.elseIf(sampleMode `==` 1.const) {
                                step set floor(step) + 1F.const
                            }
                            step /= relStepSize
                            step += -info.struct.headOffset.ksl

                            `if`((0F.const `{=` step) and (step `{=` maxStepFromHead)) {
                                val u = step + info.struct.headPos.ksl.toFloat1()
                                `return`(_sampleDataTex(tex, u).xyz)
                            }.elseIf(step `{` 0F.const) { // extrapolate forward
                                val endPoint =
                                    _sampleDataTex(tex, info.struct.headPos.ksl.toFloat1())
                                val lastToEndPoint =
                                    _sampleDataTex(tex, info.struct.headPos.ksl.toFloat1() + 1F.const)
                                `return`(mix(lastToEndPoint.xyz, endPoint.xyz, 1F.const + -step))
                            }.`else` { // extrapolate backward
                                val endPoint =
                                    _sampleDataTex(tex, info.struct.tailPos.ksl.toFloat1())
                                val lastToEndPoint =
                                    _sampleDataTex(tex, info.struct.tailPos.ksl.toFloat1() - 1F.const)
                                `return`(mix(lastToEndPoint.xyz, endPoint.xyz, (1F.const + (step - maxStepFromHead))))
                            }

                            NaN3 // should never reach here
                        }
                    }

                    val sampleData = functionFloat3("sampleData") {
                        val step = paramFloat1("step")
                        val sampleMode = paramInt1("sampleMode")
                        body {
                            val result = float3Var(_sampleData(uDataTex.ksl, uDataInfo, step, sampleMode), "result")
                            `if`(isNan(uRefMix.ksl).not()) {
                                val ref = float3Var(_sampleData(uRefDataTex.ksl, uRefDataInfo, step, sampleMode), "ref")
                                `if`(uRefMix.ksl `!=` 0F.const) {
                                    ref set mix(
                                        ref,
                                        _sampleData(uAlterRefDataTex.ksl, uAlterRefDataInfo, step, sampleMode),
                                        uRefMix.ksl
                                    )
                                }
                                result -= ref
                            }
                            result
                        }
                    }

                    main {
                        val vertexIndex = int1Var(inVertexIndex.toInt1(), "vertexIndex")
                        val nodeIndex = vertexIndex / 2.const
                        val step = iStep.input
                        step set floor(uStart.ksl + nodeIndex.toFloat1())

                        `if`((step - uEnd.ksl) `}=` 1F.const) {
                            step set NaN // tells the fragment shader to discard, in case the line below doesn't discard the primitive
                            outPosition set NaN4 // uhh... one way of discarding the primitive...?
                        }.`else` {
                            val mvp = mvpMatrix().matrix
                            val camData = cameraData()
                            val ar = camData.viewport.z / camData.viewport.w

                            step set clamp(step, uStart.ksl, uEnd.ksl)

                            val pos = sampleData(step, 0.const)
                            val prevPos = sampleData(step, -1.const)
                            val nextPos = sampleData(step, 1.const)

                            val shiftDir = float1Var(name = "shiftDir").apply {
                                `if`((vertexIndex.rem(2.const)) `==` 0.const) { set(-1F.const) }.`else` { set(1F.const) }
                            }
                            val lineWidthPort = float1Port("lineWidth", uBaseLineWidth.ksl)

                            // project positions and compute 2d directions between prev, current and next points
                            val projPos = float4Var(mvp * float4Value(pos, 1f))
                            val projPrv = float4Var(mvp * float4Value(prevPos, 1f))
                            val projNxt = float4Var(mvp * float4Value(nextPos, 1f))

                            val s = float2Var(projNxt.xy / projNxt.w - projPos.xy / projPos.w)
                            val r = float2Var(projPos.xy / projPos.w - projPrv.xy / projPrv.w)
                            s set normalize(s * float2Value(ar, 1f.const) * sign(projPos.w * projNxt.w))
                            r set normalize(r * float2Value(ar, 1f.const) * sign(projPos.w * projPrv.w))

                            // compute prev / next edge end points: rotate directions by 90°
                            val p = float2Var(rotate90(r, shiftDir))
                            val q = float2Var(rotate90(s, shiftDir))

                            // compute intersection points of prev and next edge
                            val x = float2Var((p + q) * 0.5F.const)
                            val rCrossS = float1Var(cross2(r, s))
                            `if`(abs(rCrossS) gt 0.001f.const) {
                                // lines are neither collinear nor parallel
                                val t = float1Var(clamp(cross2(q - p, s) / rCrossS, -5f.const, 5f.const))
                                x set p + t * r
                            }

                            x.x *= 1f.const / ar
                            projPos.xy += (x * lineWidthPort / camData.viewport.w) * projPos.w
                            outPosition set projPos
                        }
                    }
                }
                fragmentStage {
                    main {
                        `if`(isNan(iStep.output)) { discard() }
                        val baseColor = float4Port("baseColor", uBaseColor.ksl)
                        val outRgb = float3Var(baseColor.rgb)
                        if (pipelineConfig.blendMode == BlendMode.BLEND_PREMULTIPLIED_ALPHA) {
                            outRgb set outRgb * baseColor.a
                        }
                        colorOutput(outRgb, baseColor.a)
                    }
                }
            }
        }
    }
}

private val DUMMY_TEXTURE = StorageTexture2d(1, 1, TexFormat.RGBA_F32, name = "Dummy Trail Data Texture")