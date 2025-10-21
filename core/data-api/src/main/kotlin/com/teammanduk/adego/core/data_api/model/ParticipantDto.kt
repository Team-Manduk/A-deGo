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
    val etaInSeconds: Int = 0,      // 남은 시간 (초)
    val distanceInMeters: Int = 0,  // 남은 거리 (미터)
    val polyline: String = "",       // 인코딩된 경로
    val updatedAt: Long = 0L         // 업데이트 시간
)
