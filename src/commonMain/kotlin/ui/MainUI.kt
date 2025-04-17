package ui

import de.fabmax.kool.KoolContext
import de.fabmax.kool.math.PI_F
import de.fabmax.kool.modules.ksl.KslUnlitShader
import de.fabmax.kool.modules.ksl.blocks.TexCoordAttributeBlock
import de.fabmax.kool.modules.ksl.blocks.texCoordAttributeBlock
import de.fabmax.kool.modules.ksl.lang.*
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.pipeline.Attribute
import de.fabmax.kool.pipeline.CullMethod
import de.fabmax.kool.scene.Mesh
import de.fabmax.kool.scene.geometry.IndexedVertexList
import de.fabmax.kool.scene.geometry.Usage
import de.fabmax.kool.util.*
import universe.CelestialBody
import universe.Universe
import universe.content.Earth
import universe.content.Sun
import utils.*
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.withSign

class MainUI(private val solarSystem: Universe) : BaseReleasable() {
    private var time
        get() = solarSystem.time
        set(value) {
            solarSystem.time = value
            utcTimeState.set(value.j2000.utc)
        }
    private val utcTimeState = mutableStateOf(time.j2000.utc)

    val cameraControl = MainCameraControl(solarSystem.scene.mainRenderPass.defaultView)
        .apply { solarSystem.scene.onUpdate { update() } }

    private lateinit var timeControl: TimeControl

    val cbToTrail = mutableMapOf<CelestialBody, Trail>()

    private val trailManager = TrailManager().apply {
        solarSystem.scene.onUpdate { update() }
        solarSystem.forEach { body ->
            trails += Trail(body).also {
                solarSystem.scene += it.meshInstances[0]
            }.also { cbToTrail[body] = it }
        }
    }

    init {
        trailManager.trails.forEach {
            it.meshInstances[0].ref = cbToTrail[solarSystem[Sun]!!]
            it.meshInstances[0].alterRef = cbToTrail[solarSystem[Earth]!!]
        }
    }

    var test = 0

    private fun getSizes() = Sizes.medium(
        normalText = MsdfFont(MsdfFont.FONT_UI_DATA, sizePts = 15F),
        largeText = MsdfFont(MsdfFont.FONT_UI_DATA, sizePts = 20F)
    )

    val hudSurface = UiSurface(name = "HudSurface").apply {
        onUpdate { triggerUpdate() }
        content = {
            surface.sizes = getSizes()

            viewport.modifier.layout(FreeLayout)

//            for (pl in listOf<CelestialBody>(solarSystem.earth, solarSystem.mercury, solarSystem.jupiter).shuffled()) {
//                val pos = pl.toGlobalCoords(MutableVec3d())
//                if (solarSystem.camera.dataD.globalLookDir dot (pos - solarSystem.camera.dataD.globalPos) <= 0.0) continue
//                Button("${pl.name} Test ${test++}", scopeName = pl.name) {
//                    val spos = MutableVec3d()
//                        .also {
//                            solarSystem.camera.projectViewport(
//                                pos,
//                                solarSystem.mainRenderPass.viewport,
//                                it
//                            )
//                        }
//                    modifier.free(spos.xy, AlignmentX.Center, AlignmentY.Center)
//                }
//            }
        }
    }

    val hudUiSurface = UiSurface(name = "HudUiSurface").apply {
        content = {
            surface.sizes = getSizes()

            Column {
                modifier
                    .alignY(AlignmentY.Bottom)
                    .width(Grow.Std)

                val timeText = utcTimeState.use().feasibleInstant?.toString()
                    ?: "UTC+${utcTimeState.value.value}"
                Text(timeText) {
                    modifier
                        .alignX(AlignmentX.Center)
                }

                timeControl = uiNode.createChild("TimeControl", TimeControl::class, ::TimeControl).apply {
                    setup()
                    modifier
                        .alignX(AlignmentX.Center)
                        .size(1000.dp, 50.dp)
                        .background(RectBackground(MdColor.GREY.withAlpha(0.3F)))
                }
            }
        }
    }

    val scene = UiScene {
        this += hudSurface
        this += hudUiSurface
    }

    private inner class TrailManager {
        val trails = mutableListOf<Trail>()

        fun update() {
            trails.forEach {
                it.currentTime = solarSystem.time
                it.endTime = solarSystem.time
                it.startTime = max(IntFract.ZERO, solarSystem.time - 100 * 365.25 * 24 * 3600)
//                it.startTime = it.endTime - IntFract(3600L * 24 * 365 * 100)
//                it.startTime = it.endTime - IntFract(3600L * 24 * 365)
//                it.meshInstances[0].refMix = sin(Time.gameTime) * .5 + .5
            }
            trails.forEach { it.update() }
        }
    }

    private inner class TimeControl(parent: UiNode?, surface: UiSurface) : SlottedReboundSliderNode(parent, surface) {
        init {
            solarSystem.scene.onUpdate {
                time += value.value * Time.deltaT
            }

            val TO_ONE = 0.2
            posToValue = FwdInvFunction(
                forward = {
                    val x = abs(it)
                    (if (x > TO_ONE) (10.0.pow(x - TO_ONE)) else (x * (1.0 / TO_ONE))).withSign(it)
                },
                inverse = {
                    val x = abs(it)
                    (if (x > 1.0) (log10(x) + TO_ONE) else (x * TO_ONE)).withSign(it)
                }
            )

            val MIN = 60.0
            val HOUR = 3600.0
            val DAY = 24.0 * HOUR
            val MONTH = 30.0 * DAY
            val YEAR = 365.25 * DAY
            val DECADE = 10.0 * YEAR
            val CENTURY = 10.0 * DECADE
            slots = listOf(
                1.0, 10.0, 30.0,
                MIN, 10 * MIN, 30 * MIN,
                HOUR, 3 * HOUR, 10 * HOUR,
                DAY, 3 * DAY, 10 * DAY,
                MONTH, 6 * MONTH,
                YEAR, 3 * YEAR,
                DECADE, 5 * DECADE,
                CENTURY
            ).let { it.map { -it }.reversed() + it }
        }

        private val railAnimation = object : Mesh(
            IndexedVertexList(Attribute.POSITIONS, Attribute.TEXTURE_COORDS),
            name = "RailAnimationMesh"
        ) {
            init {
                shader = KslUnlitShader {
                    pipeline {
                        cullMethod = CullMethod.NO_CULLING
                    }
                    color { uniformColor(uniformName = "uBaseColor") }

                    modelCustomizer = {
                        val texCoordBlock: TexCoordAttributeBlock
                        vertexStage {
                            main {
                                texCoordBlock = texCoordAttributeBlock()
                            }
                        }
                        fragmentStage {
                            main {
                                val uOffset = uniformFloat1("uOffset")
                                val colorPort = getFloat4Port("baseColor")
                                val color = float4Var(colorPort.input.input)
                                val uEnd = uniformFloat1("uEnd")
                                val uv = texCoordBlock.getTextureCoords()

                                val d = float1Var(uv.x, "d")

                                // arrows
                                d += abs(0.5F.const - uv.y) * 1F.const
                                `if`(fract(d / 2F.const - uOffset) `{` 0.5F.const) {
                                    color.a set 0F.const
                                }

                                // fade in
                                color.a *= min(uv.x / 8F.const, 1F.const)

                                `if`(distance(uv, float2Value(uEnd, 0.5F.const)) `{` 0.5F.const) {
                                    color.a set sin(fract(uEnd / 2F.const - uOffset) * (PI_F * 2F).const) * 0.3F.const + 0.7F.const
                                }.`else` {
                                    `if`(uv.x `}` uEnd) { color.a set 0F.const }
                                }

                                colorPort.input(color)
                            }
                        }
                    }
                }
            }

            private var uBaseColor by shader!!.uniformColor("uBaseColor", Color.RED)
            private var uOffset by shader!!.uniform1f("uOffset")
            private var uEnd by shader!!.uniform1f("uEnd")

            private var offset = 0.0

            init {
                geometry.usage = Usage.DYNAMIC
                onUpdate {
                    val railY = slotInsertionToPx(0.0).toFloat()
                    val posCenterPx = posToPx(0.0).toFloat()

                    generate {
                        withTransform {
                            translate(leftPx, topPx, 0f)
                            rect {
                                isCenteredOrigin = false
                                origin.x = posCenterPx
                                origin.y = railY - lineWidth.px / 2F
                                size.x = (posToPx(pos.value) - posCenterPx).toFloat()
                                size.y = lineWidth.px

                                generateTexCoords(1F)
                                var texCoordX = abs(size.x) / size.y

                                uEnd = texCoordX

                                // add an extra tail so that the shader can render some interesting effect at the end
                                // (instead of cutting off suddenly)
                                size.x += size.y.withSign(size.x)
                                texCoordX += 1F

                                texCoordUpperRight.x = texCoordX
                                texCoordLowerRight.x = texCoordX
                            }
                        }
                    }

                    uBaseColor = if (pos.value >= 0.0) MdColor.LIGHT_GREEN else MdColor.RED tone 450
                    offset += abs(pos.value) * Time.deltaT
                    uOffset = offset.rem(1.0).toFloat()
                }
            }
        }

        override fun render(ctx: KoolContext) {
            super.render(ctx)

            surface.getMeshLayer(modifier.zLayer + 0)
                .addCustomLayer("RailAnimation") { railAnimation }
        }
    }
}