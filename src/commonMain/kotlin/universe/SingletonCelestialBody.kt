package universe

import utils.Companioned

abstract class SingletonCelestialBody<Self : SingletonCelestialBody<Self, CP>, CP : SingletonCelestialBody.CompanionObj<Self>> :
    CelestialBody(), Companioned<CP> {

    abstract class CompanionObj<@Suppress("unused") CLS : SingletonCelestialBody<*, *>>
}