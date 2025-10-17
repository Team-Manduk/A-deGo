package com.teammanduk.adego.core.model

/**
 * 참여자 정보 (동적 데이터 - 위치/경로 실시간 변경)
 */
data class Participant(
    val userId: String,
    val name: String,
    val profileColor: String, // 색상 hex 코드 (예: "#E53935")
    val location: ParticipantLocation? = null,
    val route: ParticipantRoute? = null
)
