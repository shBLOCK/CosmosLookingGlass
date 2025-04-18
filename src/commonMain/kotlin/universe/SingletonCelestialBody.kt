package universe

import utils.Companioned

abstract class SingletonCelestialBody<Self : SingletonCelestialBody<Self, CP>, CP : SingletonCelestialBody.CompanionObj<Self>> :
    CelestialBody(), Companioned<CP> {
    init {
        name = this::class.simpleName ?: makeNodeName(SingletonCelestialBody::class.simpleName!!)
    }

    abstract class CompanionObj<@Suppress("unused") CLS : SingletonCelestialBody<*, *>>
}