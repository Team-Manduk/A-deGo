package com.teammanduk.adego.feature.map.model

import com.teammanduk.adego.core.model.ParticipantLocation
import com.teammanduk.adego.feature.map.util.NicknameGenerator

data class MapUiState(
    val roomId: String = "",
    val userId: String = "",
    val room: RoomUiModel? = null,
    val participants: List<ParticipantUiModel> = emptyList(),
    val selectedParticipantIndex: Int = 0,
    val isLocationTrackingActive: Boolean = false,
    val isCheckingRoom: Boolean = true,
    val error: String? = null,
    val showInviteDialog: Boolean = false,
    val showRouteDialog: Boolean = false,
    val showSelectStartPlace: Boolean = false,
    val showSearchPlace: Boolean = false,
    val startPlace: com.teammanduk.adego.core.model.Place? = null,
    val searchedRoutes: List<RouteUiModel> = emptyList(),
    val selectedRouteIndex: Int? = null,
    val isSearchingRoute: Boolean = false,
    val userName: String = "",
    val showUserNameDialog: Boolean = true,
) {
    val currentUser: ParticipantUiModel?
        get() = participants.find { it.userId == userId }
}