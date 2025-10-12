package com.teammanduk.adego.core.data_api.model

data class RoomDto(
    val roomId: String = "",
    val roomName: String = "",
    val destination: PlaceDto = PlaceDto(),
    val dateTime: String = "",
    val createdBy: String = "",
    val createdAt: Long = 0L
)
