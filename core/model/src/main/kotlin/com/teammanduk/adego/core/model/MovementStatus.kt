package com.teammanduk.adego.core.model

/**
 * 사용자의 이동 상태
 */
enum class MovementStatus {
    /**
     * 경로 선택 전 - 아직 경로를 선택하지 않은 상태
     */
    NOT_STARTED,

    /**
     * 출발 전 - 경로는 선택했지만 아직 출발하지 않은 상태
     * (출발지에서 일정 거리 이내)
     */
    READY,

    /**
     * 이동 중 - 출발지를 벗어나 목적지로 이동 중
     */
    IN_PROGRESS,

    /**
     * 곧 도착 - 목적지 근처 (예: 500m 이내)
     */
    ARRIVING_SOON,

    /**
     * 도착 완료 - 목적지에 도착
     */
    ARRIVED
}
