package com.teammanduk.adego.core.model

/**
 * 참여자 경로 정보 (현재 위치 → 목적지)
 *
 * Domain 모델로서 원본 숫자 데이터만 포함합니다.
 * UI 포맷팅(문자열 변환)은 Presentation Layer에서 수행합니다.
 */
data class ParticipantRoute(
    val durationInSeconds: Int, // 남은 시간 (초)
    val distanceInMeters: Int, // 남은 거리 (미터)
    val polyline: String, // 인코딩된 경로
    val updatedAt: Long, // 업데이트 시간
    val currentSubPathIndex: Int = -1, // 현재 진행 중인 구간 인덱스
    val progressInCurrentSubPath: Double = 0.0 // 현재 구간 내 진행률 (0.0 ~ 1.0)
)
