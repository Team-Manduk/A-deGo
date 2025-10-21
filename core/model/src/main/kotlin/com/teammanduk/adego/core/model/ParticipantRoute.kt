package com.teammanduk.adego.core.model

/**
 * 참여자 경로 정보 (현재 위치 → 목적지)
 *
 * Firebase를 통해 실시간으로 동기화되는 경로 정보.
 * 전체 경로 상세 정보(Route)는 Room DB에 저장됨.
 */
data class ParticipantRoute(
    val eta: String, // "15분"
    val distance: String, // "3.2km"
    val polyline: String, // 인코딩된 경로
    val durationInSeconds: Int = 0,
    val distanceInMeters: Int = 0,
    val updatedAt: Long,

    // 경로 진행률 추적 정보
    val currentSubPathIndex: Int? = null, // 현재 진행 중인 SubPath 인덱스
    val progressInCurrentSubPath: Double? = null, // 현재 SubPath 내 진행률 (0.0 ~ 1.0)
    val traveledDistance: Double? = null, // 현재까지 이동한 총 거리 (미터)
    val remainingDistance: Double? = null // 남은 거리 (미터)
)
