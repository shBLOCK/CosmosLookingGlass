package universe

import de.fabmax.kool.pipeline.RenderPass
import utils.AstroTime
import de.fabmax.kool.scene.Scene
import de.fabmax.kool.util.Time
import dynamics.SolarSystemDynModel
import dynamics.SolarSystemKeplerModel3000BC3000AD

class SolarSystemScene : Scene() {
    init {
        mainRenderPass.isDoublePrecision = true
    }

    var time = AstroTime(0.0)

    var dynModel: SolarSystemDynModel = SolarSystemKeplerModel3000BC3000AD() //TODO: selectable model

    val sun = Sun().also { it.dynModelProvider = dynModel::sun }.also(this::addNode)
    val mercury = Mercury().also { it.dynModelProvider = dynModel::mercury }.also(this::addNode)
    val venus = Venus().also { it.dynModelProvider = dynModel::venus }.also(this::addNode)
    val earth = Earth().also { it.dynModelProvider = dynModel::earth }.also(this::addNode)
    val moon = Moon().also { it.dynModelProvider = dynModel::moon }.also(this::addNode)
    val mars = Mars().also { it.dynModelProvider = dynModel::mars }.also(this::addNode)
    val jupiter = Jupiter().also { it.dynModelProvider = dynModel::jupiter }.also(this::addNode)
    val saturn = Saturn().also { it.dynModelProvider = dynModel::saturn }.also(this::addNode)
    val uranus = Uranus().also { it.dynModelProvider = dynModel::uranus }.also(this::addNode)
    val neptune = Neptune().also { it.dynModelProvider = dynModel::neptune }.also(this::addNode)

    override fun update(updateEvent: RenderPass.UpdateEvent) {
        time += Time.deltaT.toDouble() * 60 * 60 * 24 * 6
        dynModel.seek(time)

        super.update(updateEvent)
    }
}