package com.teammanduk.adego.core.model

/**
 * 참여자 경로 정보 (현재 위치 → 목적지)
 *
 * Firebase를 통해 실시간으로 동기화되는 경로 정보.
 * 전체 경로 상세 정보(Route)는 Room DB에 저장됨.
 *
 * UI에서 포맷팅하여 표시:
 * - etaInSeconds → "15분", "1시간 30분"
 * - distanceInMeters → "3.2km", "850m"
 */
data class ParticipantRoute(
    val etaInSeconds: Int,      // 남은 시간 (초)
    val distanceInMeters: Int,  // 남은 거리 (미터)
    val polyline: String,       // 인코딩된 경로
    val updatedAt: Long         // 업데이트 시간
)
