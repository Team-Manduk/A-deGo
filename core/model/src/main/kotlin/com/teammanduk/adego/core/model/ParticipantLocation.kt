package com.teammanduk.adego.core.model

/**
 * 참여자 위치 정보
 */
data class ParticipantLocation(
    val latitude: Double,
    val longitude: Double,
    val updatedAt: Long,
    val accuracy: Float = 0f
)
