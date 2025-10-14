package com.teammanduk.adego.feature.map.model

import com.teammanduk.adego.core.common.ext.toMeetingTimeFormat
import com.teammanduk.adego.core.model.Place
import com.teammanduk.adego.core.model.Room

data class RoomUiModel(
    val roomId: String,
    val roomName: String,
    val destination: Place,
    val meetingTime: String
)

fun Room.toUiModel(): RoomUiModel = RoomUiModel(
    roomId = this.roomId,
    roomName = this.roomName,
    destination = this.destination,
    meetingTime = this.dateTime.toMeetingTimeFormat()
)
