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
        val userId: String,
        val userName: String? = null  // 세션 복구 시 사용
    ) : Route

    @Serializable
    data class SelectStartPlace(
        val roomId: String,
        val userId: String,
        val destLat: Double,
        val destLng: Double
    ) : Route

    @Serializable
    data class SelectRoute(
        val roomId: String,
        val userId: String,
        val startLat: Double,
        val startLng: Double,
        val destLat: Double,
        val destLng: Double
    ) : Route

    @Serializable
    data object RouteGuidance : Route
}