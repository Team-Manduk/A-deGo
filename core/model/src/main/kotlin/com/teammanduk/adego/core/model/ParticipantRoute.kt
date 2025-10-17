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
    val updatedAt: Long,
    val currentSubPathIndex: Int = -1, // 현재 진행 중인 구간 인덱스
    val progressInCurrentSubPath: Double = 0.0 // 현재 구간 내 진행률 (0.0 ~ 1.0)
)
