package universe.content

import de.fabmax.kool.Assets
import de.fabmax.kool.modules.ksl.KslPbrShader
import de.fabmax.kool.modules.ksl.lang.KslInterStageInterpolation
import de.fabmax.kool.modules.ksl.lang.getFloat4Port
import de.fabmax.kool.pipeline.*
import de.fabmax.kool.scene.addColorMesh
import de.fabmax.kool.scene.addMesh
import de.fabmax.kool.scene.geometry.MeshBuilder
import de.fabmax.kool.util.Color
import kotlinx.coroutines.launch
import universe.CelestialBody
import utils.SingleColorTextureCube
import utils.loadTextureCube

fun CelestialBody.setupSimpleSpherical(
    radius: Double,
    flattening: Double = 0.0
) {
    addColorMesh {
        generate {
            color = themeColor
            uvSphere {
                this.radius = (radius).toFloat()
//                this.radius = 10000000e3f
                steps = 64
            }
        }
        shader = KslPbrShader {
            color { vertexColor() }
            metallic(0f)
            roughness(.8f)
        }
    }
}

fun CelestialBody.setupTexturedSpherical(
    radius: Double,
    flattening: Double = 0.0,
    cubemap: String
) {
    val aCubeMapCoords = Attribute("aCubeMapCoords", GpuType.Float3)
    addMesh(
        Attribute.POSITIONS, Attribute.NORMALS, aCubeMapCoords
    ) {
        generate {
            run {
                val icoGenerator = MeshBuilder.IcoGenerator()
                icoGenerator.subdivide(4)
                val i0 = geometry.numVertices
                for (vert in icoGenerator.verts) {
                    vertex {
                        position.set(vert).mul(radius.toFloat())
//                        position.set(vert).mul(10000000e3f)
                        normal.set(vert).norm()
                        getVec3fAttribute(aCubeMapCoords)!!.set(vert).norm()
                    }
                }
                for (i in icoGenerator.faces.indices step 3) {
                    addTriIndices(
                        i0 + icoGenerator.faces[i],
                        i0 + icoGenerator.faces[1 + i],
                        i0 + icoGenerator.faces[2 + i]
                    )
                }
            }
        }
        shader = KslPbrShader {
            color { constColor(Color.MAGENTA) }
            metallic(0f)
            roughness(.8f)
            modelCustomizer = {
                val vCubeMapCoords = interStageFloat3("vCubeMapCoords", KslInterStageInterpolation.Smooth)
                vertexStage {
                    main {
                        vCubeMapCoords.input set vertexAttribFloat3(aCubeMapCoords)
                    }
                }
                fragmentStage {
                    main {
                        val colorPort = getFloat4Port("baseColor")
                        val color = sampleTexture(textureCube("uCubeMapColor"), normalize(vCubeMapCoords.output))
                        colorPort.input(color)
                    }
                }
            }
        }.apply {
            val textureCubeBinding = textureCube(
                "uCubeMapColor",
                SingleColorTextureCube(themeColor),
                SamplerSettings().linear()
            )
            Assets.launch {
                Assets.defaultLoader.loadTextureCube(
                    cubemap,
                    TexFormat.RGBA,
                    MipMapping.Full,
                    SamplerSettings().linear()
                ).onSuccess {
                    textureCubeBinding.set(it)
                }
            }
        }
    }
}