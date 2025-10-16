package com.teammanduk.adego.feature.create.model

import com.teammanduk.adego.core.model.Place

sealed interface CreateIntent {
    data class SelectPlace(val place: Place) : CreateIntent
    data class UpdateMeetingPlaceName(val placeName: String) : CreateIntent
    data class UpdateRoomName(val roomName: String) : CreateIntent
    data class SelectDate(val dateMillis: Long) : CreateIntent
    data class SelectTime(val hour: Int, val minute: Int) : CreateIntent
    data class CreateRoom(val userId: String, val userName: String) : CreateIntent
    data object ClearError : CreateIntent

    // Navigation
    data object NavigateToSelectPlace : CreateIntent
    data object NavigateBack : CreateIntent
}
