package com.teammanduk.adego.feature.map.model

data class MapUiState(
    val room: RoomUiModel? = null,
    val participants: List<ParticipantUiModel> = emptyList(),
    val selectedParticipantIndex: Int = 0,
    val isLocationTrackingActive: Boolean = false,
    val isInitialLocationLoaded: Boolean = false,
    val showUserNameInput: Boolean = true,
    val userName: String = "",
    val error: String? = null
)
