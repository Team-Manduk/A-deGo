package com.teammanduk.adego.feature.map.model

import com.teammanduk.adego.core.model.ParticipantLocation

data class MapUiState(
    val room: RoomUiModel? = null,
    val participants: List<ParticipantUiModel> = emptyList(),
    val selectedParticipantIndex: Int = 0,
    val myLocation: ParticipantLocation? = null,
    val isLocationTrackingActive: Boolean = false,
    val isInitialLocationLoaded: Boolean = false,
    val showUserNameInput: Boolean = true,
    val userName: String = "",
    val error: String? = null
)
