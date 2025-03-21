package ui

import de.fabmax.kool.math.MutableVec3d
import de.fabmax.kool.modules.ui2.AlignmentX
import de.fabmax.kool.modules.ui2.AlignmentY
import de.fabmax.kool.modules.ui2.Button
import de.fabmax.kool.modules.ui2.UiScene
import de.fabmax.kool.modules.ui2.UiSurface
import de.fabmax.kool.modules.ui2.layout
import de.fabmax.kool.util.Time
import dynamics.CelestialDynModel
import dynamics.DynModel
import dynamics.DynModelBase
import universe.CelestialBody
import universe.SolarSystemScene
import kotlin.math.sin

class Hud(
    val solarSystem: SolarSystemScene
) {
    private val trailManager = TrailManager().apply {
        solarSystem.celestialBodies.forEach { addTrace(it) }

        reference = CelestialDynModel.Blending(solarSystem.sun.dynModel, solarSystem.earth.dynModel)

        solarSystem += this.node
    }

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

        onUpdate {
            trailManager.endTime = solarSystem.time
            (trailManager.reference as CelestialDynModel.Blending).apply { t = sin(Time.gameTime)*.5+.5 }
            trailManager.rebuild()
        }
    }
}