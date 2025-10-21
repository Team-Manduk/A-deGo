package com.teammanduk.adego.core.model

/**
 * 사용자의 이동 상태
 *
 * @property distanceThresholdMeters 해당 상태를 판단하는 거리 임계값 (미터)
 * - null인 경우 거리 임계값이 적용되지 않음
 */
enum class MovementStatus(val distanceThresholdMeters: Int?) {
    /**
     * 경로 선택 전 - 아직 경로를 선택하지 않은 상태
     */
    NOT_STARTED(null),

    /**
     * 출발 전 - 경로는 선택했지만 아직 출발하지 않은 상태
     * (출발지에서 200m 이내)
     */
    READY(200),

    /**
     * 이동 중 - 출발지를 벗어나 목적지로 이동 중
     */
    IN_PROGRESS(null),

    /**
     * 곧 도착 - 목적지 근처 (500m 이내)
     */
    ARRIVING_SOON(500),

    /**
     * 도착 완료 - 목적지에 도착 (100m 이내)
     */
    ARRIVED(100);

    companion object {
        /**
         * 도착 판단 거리 임계값
         */
        const val ARRIVAL_THRESHOLD = 100

        /**
         * 곧 도착 판단 거리 임계값
         */
        const val ARRIVING_SOON_THRESHOLD = 500

        /**
         * 출발 전 판단 거리 임계값
         */
        const val READY_THRESHOLD = 200
    }
}
