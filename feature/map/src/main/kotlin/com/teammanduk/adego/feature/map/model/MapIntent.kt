package com.teammanduk.adego.feature.map.model

sealed interface MapIntent {
    data class SelectParticipant(val index: Int) : MapIntent
    data object ClearError : MapIntent
}
