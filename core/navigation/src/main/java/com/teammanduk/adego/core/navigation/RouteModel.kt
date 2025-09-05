package com.teammanduk.adego.core.navigation

import kotlinx.serialization.Serializable

sealed interface Route {
    @Serializable
    data object Home: Route
}