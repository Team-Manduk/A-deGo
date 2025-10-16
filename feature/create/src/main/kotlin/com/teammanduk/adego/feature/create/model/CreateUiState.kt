package com.teammanduk.adego.feature.create.model

import com.teammanduk.adego.core.model.Place

data class CreateUiState(
    val currentStep: CreateStep = CreateStep.PLACE_SELECTION,
    val selectedPlace: Place? = null,
    val meetingPlaceName: String = "",
    val roomName: String = "",
    val selectedDate: Long? = null,
    val selectedHour: Int? = null,
    val selectedMinute: Int? = null,
    val isCreating: Boolean = false,
    val createdRoomInfo: CreatedRoomInfo? = null,
    val error: String? = null
)

data class CreatedRoomInfo(
    val roomId: String,
    val userId: String
)
