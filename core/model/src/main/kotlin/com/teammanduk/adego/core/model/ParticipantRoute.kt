package com.teammanduk.adego.core.model

/**
 * 참여자 경로 정보 (현재 위치 → 목적지)
 */
data class ParticipantRoute(
    val eta: String, // "15분"
    val distance: String, // "3.2km"
    val polyline: String, // 인코딩된 경로
    val durationInSeconds: Int = 0,
    val distanceInMeters: Int = 0,
    val updatedAt: Long
)
