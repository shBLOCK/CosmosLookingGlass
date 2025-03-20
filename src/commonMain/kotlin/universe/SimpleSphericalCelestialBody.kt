package universe

import de.fabmax.kool.modules.ksl.KslPbrShader
import de.fabmax.kool.scene.addColorMesh
import de.fabmax.kool.util.Color

open class SimpleSphericalCelestialBody(
    val radius: Double,
    val flattening: Double = 0.0,
    themeColor: Color = Color.WHITE
) : CelestialBody(themeColor) {
    init {
        addColorMesh {
            generate {
                color = this@SimpleSphericalCelestialBody.themeColor
                println(color)
                uvSphere {
//                    radius = this@SimpleSphericalCelestialBody.radius.toFloat()
                    radius = 10000000e3f
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
}