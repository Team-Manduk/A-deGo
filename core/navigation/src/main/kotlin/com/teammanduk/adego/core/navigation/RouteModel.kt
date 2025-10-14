package com.teammanduk.adego.core.navigation

import kotlinx.serialization.Serializable

sealed interface Route {
    @Serializable
    data object Home : Route

    @Serializable
    data object Create : Route

    @Serializable
    data object SelectPlace : Route

    @Serializable
    data object SearchPlace : Route

    @Serializable
    data class Map(
        val roomId: String,
        val userId: String
    ) : Route
}