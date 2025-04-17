package universe.content

import de.fabmax.kool.modules.ksl.KslPbrShader
import de.fabmax.kool.scene.addColorMesh
import universe.CelestialBody

fun CelestialBody.setupSimpleSpherical(
    radius: Double,
    flattening: Double = 0.0
) {
    addColorMesh {
        generate {
            color = themeColor
            uvSphere {
//                this.radius = radius.toFloat()
                this.radius = 10000000e3f
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