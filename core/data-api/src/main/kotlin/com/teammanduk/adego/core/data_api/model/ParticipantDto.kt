package com.teammanduk.adego.core.data_api.model

data class ParticipantDto(
    val userId: String = "",
    val name: String = "",
    val profileColor: String = "",
    val location: ParticipantLocationDto? = null,
    val route: ParticipantRouteDto? = null
)

data class ParticipantLocationDto(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val updatedAt: Long = 0L,
    val accuracy: Float = 0f
)

data class ParticipantRouteDto(
    val eta: String = "",
    val distance: String = "",
    val polyline: String = "",
    val durationInSeconds: Int = 0,
    val distanceInMeters: Int = 0,
    val updatedAt: Long = 0L
)
