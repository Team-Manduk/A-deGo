package com.teammanduk.adego.feature.map.model

sealed interface MapIntent {
    data class SelectParticipant(val index: Int) : MapIntent
    data class UpdateUserName(val userName: String) : MapIntent
    data object ConfirmUserName : MapIntent
    data object ClearError : MapIntent
}
