package ui

import de.fabmax.kool.modules.ui2.UiScene
import de.fabmax.kool.modules.ui2.UiSurface
import de.fabmax.kool.modules.ui2.layout
import de.fabmax.kool.util.Time
import universe.CelestialBody
import universe.SolarSystemScene
import kotlin.math.sin

class Hud(
    val solarSystem: SolarSystemScene
) {
    val cbToTrail = mutableMapOf<CelestialBody, Trail>()

    private val trailManager = TrailManager().apply {
        solarSystem.onUpdate { update() }
        solarSystem.celestialBodies.forEach { body ->
            trails += Trail(body).also {
                solarSystem += it.meshInstances[0]
            }.also { cbToTrail[body] = it }
        }
    }

    init {
        trailManager.trails.forEach {
            it.meshInstances[0].ref = cbToTrail[solarSystem.sun]
            it.meshInstances[0].alterRef = cbToTrail[solarSystem.earth]
        }
    }

    var test = 0

    val hud: UiSurface = UiSurface(name = "HudSurface").also { hudSurface ->
        hudSurface.apply {
            onUpdate { triggerUpdate() }
        }
        hudSurface.content = {
            hudSurface.viewport.modifier.layout(FreeLayout)

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

    val scene = UiScene {
        this += hud
    }

    private inner class TrailManager {
        val trails = mutableListOf<Trail>()

        fun update() {
            trails.forEach {
                it.currentTime = solarSystem.time
                it.endTime = solarSystem.time
//                it.startTime = it.endTime - IntFract(3600L*24*365*50)
                it.meshInstances[0].refMix = sin(Time.gameTime) * .5 + .5
            }
            trails.forEach { it.update() }
        }
    }
}