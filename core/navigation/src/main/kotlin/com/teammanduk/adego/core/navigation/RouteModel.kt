package com.teammanduk.adego.core.navigation

import kotlinx.serialization.Serializable

sealed interface Route {
    @Serializable
    data object Home: Route

    @Serializable
    data object Create: Route

    @Serializable
    data object SelectPlace: Route

    @Serializable
    data object Map: Route
}