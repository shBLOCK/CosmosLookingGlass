package ui

import de.fabmax.kool.math.MutableVec3d
import de.fabmax.kool.modules.ui2.AlignmentX
import de.fabmax.kool.modules.ui2.AlignmentY
import de.fabmax.kool.modules.ui2.Button
import de.fabmax.kool.modules.ui2.UiScene
import de.fabmax.kool.modules.ui2.UiSurface
import de.fabmax.kool.modules.ui2.layout
import universe.CelestialBody
import universe.SolarSystemScene

class Hud(
    val solarSystem: SolarSystemScene
) {
    var test = 0

    val hud: UiSurface = UiSurface(name = "HudSurface").also { hudSurface ->
        hudSurface.apply {
            onUpdate { triggerUpdate() }
        }
        hudSurface.content = {
            hudSurface.viewport.modifier.layout(FreeLayout)

            for (pl in listOf<CelestialBody>(solarSystem.earth, solarSystem.mercury, solarSystem.jupiter).shuffled()) {
                val pos = pl.toGlobalCoords(MutableVec3d())
                if (solarSystem.camera.dataD.globalLookDir dot (pos - solarSystem.camera.dataD.globalPos) <= 0.0) continue
                Button("${pl.name} Test ${test++}", scopeName = pl.name) {
                    val spos = MutableVec3d()
                        .also {
                            solarSystem.camera.projectViewport(
                                pos,
                                solarSystem.mainRenderPass.viewport,
                                it
                            )
                        }
                    modifier.free(spos.xy, AlignmentX.Center, AlignmentY.Center)
                }
            }
        }
    }

    val scene = UiScene {
        this += hud
    }
}