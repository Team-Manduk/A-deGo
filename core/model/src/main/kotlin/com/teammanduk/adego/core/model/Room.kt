package com.teammanduk.adego.core.model

/**
 * 방 정보 (정적 데이터)
 * 참여자 정보는 별도로 실시간 구독
 */
data class Room(
    val roomId: String,
    val roomName: String,
    val destination: Place,
    val dateTime: String,
    val createdBy: String,
    val createdAt: Long
)
