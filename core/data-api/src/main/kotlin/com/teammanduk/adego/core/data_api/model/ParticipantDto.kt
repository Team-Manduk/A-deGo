package com.teammanduk.adego.core.data_api.model

data class ParticipantDto(
    val userId: String = "",
    val name: String = "",
    val profileColor: String = "",
    val location: ParticipantLocationDto? = null,
    val route: ParticipantRouteDto? = null,
    val movementStatus: String = "NOT_STARTED"
)

data class ParticipantLocationDto(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val updatedAt: Long = 0L,
    val accuracy: Float = 0f
)

/**
 * 참여자 경로 정보 (Firebase 저장용 DTO)
 *
 * 실시간 ETA/거리 정보만 Firebase에 저장.
 * 전체 경로 상세 정보는 Room DB에 저장됨.
 */
data class ParticipantRouteDto(
    val eta: String = "",
    val distance: String = "",
    val polyline: String = "",
    val durationInSeconds: Int = 0,
    val distanceInMeters: Int = 0,
    val updatedAt: Long = 0L,
    val currentSubPathIndex: Int? = null,
    val progressInCurrentSubPath: Double? = null,
    val traveledDistance: Double? = null,
    val remainingDistance: Double? = null
)
