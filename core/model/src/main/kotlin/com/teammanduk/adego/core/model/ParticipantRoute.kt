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

    // 경로 진행률 추적 정보
    val currentSubPathIndex: Int? = null, // 현재 진행 중인 SubPath 인덱스
    val progressInCurrentSubPath: Double? = null, // 현재 SubPath 내 진행률 (0.0 ~ 1.0)
    val traveledDistance: Double? = null, // 현재까지 이동한 총 거리 (미터)
    val remainingDistance: Double? = null, // 남은 거리 (미터)

    // 선택한 대중교통 경로 정보 (출발지 → 목적지 전체 경로)
    val selectedRoute: Route? = null // TMAP에서 검색한 경로 정보
)
