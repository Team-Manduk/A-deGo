package com.teammanduk.adego.core.model

/**
 * ETA 계산 결과 모델
 *
 * 경로 기반 도착 예정 시간 계산의 결과를 나타냅니다.
 */
data class EtaCalculationResult(
    /**
     * 남은 시간 (초)
     */
    val remainingTimeInSeconds: Int,

    /**
     * 남은 거리 (미터)
     */
    val remainingDistanceInMeters: Double,

    /**
     * 현재 구간 인덱스
     */
    val currentSubPathIndex: Int,

    /**
     * 현재 구간 내 진행률 (0.0 ~ 1.0)
     */
    val progressInCurrentSubPath: Double
)
