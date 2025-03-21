@file:Suppress("NOTHING_TO_INLINE")

package ui

import de.fabmax.kool.math.MutableVec3d
import de.fabmax.kool.math.MutableVec3f
import de.fabmax.kool.math.Vec3d
import de.fabmax.kool.math.set
import de.fabmax.kool.scene.Node
import de.fabmax.kool.scene.TriangulatedLineMesh
import de.fabmax.kool.util.Color
import dynamics.CelestialDynModel
import dynamics.copyTyped
import universe.CelestialBody
import utils.AstroTime
import kotlin.collections.plusAssign

class TrailManager {
    val node = Node("TrailManager")

    var step = 3600.0 * 24.0
    var startTime = AstroTime(0.0)
    var endTime = AstroTime(0.0)

    private val trails = mutableListOf<Trail>()

    var reference: CelestialDynModel? = null

    inner class Trail(val cb: CelestialBody) {
        private val lineMesh = TriangulatedLineMesh("Trail(${cb.name})")
        val node: Node get() = lineMesh

        private val tmpVec3f = MutableVec3f()
        private val tmpVec3d = MutableVec3d()

        internal fun rebuild() {
            val model = cb.dynModel.copyTyped()

            fun lineTo(model: CelestialDynModel, time: AstroTime) {
                model.seek(time)
                model.position(tmpVec3d)
                reference?.run {
                    seek(time)
                    tmpVec3d.subtract(position())
                }
                tmpVec3f.set(tmpVec3d)
                lineMesh.lineTo(tmpVec3f, color = cb.themeColor, width = 3F)
            }

            lineMesh.clear()
            var t = startTime
            while (true) {
                if (t > endTime) t = endTime
                lineTo(model, t)
                if (t == endTime) break
                t += step
            }
            lineMesh.stroke()
        }
    }

    fun rebuild() {
        startTime = endTime - 3600.0*24*365*8
        node.transform.setIdentity()
        trails.forEach(Trail::rebuild)
        node.transform.translate(reference?.run { seek(endTime); position() } ?: Vec3d.ZERO)
    }

    fun addTrace(cb: CelestialBody) = Trail(cb).also {
        trails += it
        node += it.node
    }
}