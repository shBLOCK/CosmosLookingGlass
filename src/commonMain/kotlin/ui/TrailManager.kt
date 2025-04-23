package ui

import de.fabmax.kool.scene.Node
import de.fabmax.kool.scene.TrsTransformD
import universe.CelestialBody
import utils.IntFract
import utils.combinedWith
import utils.max
import utils.min

class TrailManager {
    val mainTrailMode: MainTrailMode = MainTrailMode.OneRev()
    private val mainTrails = mutableListOf<MainTrail>()
    val mainTrailsRoot = Node("MainTrails").apply {
        transform = TrsTransformD() // use double precision transform
    }

    fun update(currentTime: IntFract) {
        mainTrails.forEach { mainTrailMode.update(currentTime, it) }

        fun TrailData.expandDataRangeToSuffice(instance: TrailInstance) {
            startTime = min(startTime, instance.startTime)
            endTime = max(endTime, instance.endTime)
        }
        mainTrails.forEach {
            val instance = it.instance
            instance.refData?.expandDataRangeToSuffice(instance)
            instance.alterRefData?.expandDataRangeToSuffice(instance)
        }

        mainTrails.forEach { it.data.update() }
        mainTrails.forEach { it.instance.update() }
    }

    data class MainTrail(
        val celestialBody: CelestialBody,
        val revolution: IntFract,
        val defaultRef: MainTrail?
    ) {
        val data = TrailData(celestialBody.dynModel, celestialBody.name)
        val instance = TrailInstance(data, celestialBody.themeColor, celestialBody.name)

        var startTime by instance::startTime.combinedWith(data::startTime)
        var endTime by instance::endTime.combinedWith(data::endTime)
        var currentTime by instance::currentTime
        var visible by instance::isVisible
    }

    operator fun plusAssign(mainTrail: MainTrail) {
        mainTrails += mainTrail
        mainTrailsRoot += mainTrail.instance
    }

    sealed class MainTrailMode {
        class OneRev : MainTrailMode() {
            override fun update(currentTime: IntFract, trail: MainTrail) {
                super.update(currentTime, trail)
                trail.currentTime = currentTime
                trail.endTime = currentTime
                trail.startTime = currentTime - trail.revolution
            }
        }

        internal open fun update(currentTime: IntFract, trail: MainTrail) {
            trail.data.dynModel = trail.celestialBody.dynModel
            trail.instance.refData = trail.defaultRef?.data
        }
    }
}