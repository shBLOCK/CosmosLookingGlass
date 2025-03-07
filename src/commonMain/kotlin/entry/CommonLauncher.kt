package entry

import de.fabmax.kool.KoolContext
import de.fabmax.kool.math.Vec3f
import de.fabmax.kool.math.deg
import de.fabmax.kool.modules.ksl.KslPbrShader
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.scene.Scene
import de.fabmax.kool.scene.addColorMesh
import de.fabmax.kool.scene.defaultOrbitCamera
import de.fabmax.kool.scene.scene
import de.fabmax.kool.util.Color
import de.fabmax.kool.util.MdColor
import de.fabmax.kool.util.Time
import de.fabmax.kool.util.debugOverlay

/**
 * Main application entry. This demo creates a small example scene, which you probably want to replace by your actual
 * game / application content.
 */
fun launchApp(ctx: KoolContext) {
    // add a hello-world demo scene
    ctx.scenes += scene {
        // enable simple camera mouse control
        defaultOrbitCamera()

        // add a spinning color cube to the scene
        addColorMesh {
            generate {
                // called once on init: generates a cube with different (vertex-)colors assigned to each side
                cube {
                    colored()
                }
            }
            onUpdate {
                // called on each frame: spins the cube around its x-axis by 45 degrees per second
                transform.rotate(45f.deg * Time.deltaT, Vec3f.X_AXIS)
            }
            // assign a shader, which uses the vertex color info
            shader = KslPbrShader {
                color { vertexColor() }
                metallic(0f)
                roughness(0.25f)
            }
        }

        // set up a single light source
        lighting.singleDirectionalLight {
            setup(Vec3f(-1f, -1f, -1f))
            setColor(Color.WHITE, 5f)
        }
    }

    ctx.scenes += scene {
        setupUiScene(Scene.DEFAULT_CLEAR_COLOR)

        addPanelSurface(colors = Colors.singleColorLight(MdColor.LIGHT_GREEN)) {
            modifier
                .size(400.dp, 300.dp)
                .align(AlignmentX.Center, AlignmentY.Center)
                .background(RoundRectBackground(colors.background, 16.dp))

            var clickCount by remember(0)
            Button("Click me!") {
                modifier
                    .alignX(AlignmentX.Center)
                    .margin(sizes.largeGap * 4f)
                    .padding(horizontal = sizes.largeGap, vertical = sizes.gap)
                    .font(sizes.largeText)
                    .onClick { clickCount++ }
            }
            Text("Button clicked $clickCount times") {
                modifier
                    .alignX(AlignmentX.Center)
            }
        }
    }

    // add the debugOverlay. provides an fps counter and some additional debug info
    ctx.scenes += debugOverlay()
}