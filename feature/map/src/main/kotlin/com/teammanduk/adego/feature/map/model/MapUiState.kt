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
    val error: String? = null,
    val showInviteDialog: Boolean = false,
    val showRouteDialog: Boolean = false,
    val showSelectStartPlace: Boolean = false,
    val showSearchPlace: Boolean = false,
    val startPlace: com.teammanduk.adego.core.model.Place? = null,
    val searchedRoutes: List<com.teammanduk.adego.core.model.Route> = emptyList(),
    val selectedRouteIndex: Int? = null,
    val isSearchingRoute: Boolean = false
)
