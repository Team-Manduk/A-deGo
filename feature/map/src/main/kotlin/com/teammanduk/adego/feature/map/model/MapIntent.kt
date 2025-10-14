package com.teammanduk.adego.feature.map.model

sealed interface MapIntent {
    data class SelectParticipant(val index: Int) : MapIntent
    data class UpdateUserName(val userName: String) : MapIntent
    data object ConfirmUserName : MapIntent
    data object ClearError : MapIntent
    data object ShowInviteDialog : MapIntent
    data object DismissInviteDialog : MapIntent
    data object ShowRouteDialog : MapIntent
    data object DismissRouteDialog : MapIntent
    data object ShowSelectStartPlace : MapIntent
    data object DismissSelectStartPlace : MapIntent
    data class StartPlaceSelected(val place: com.teammanduk.adego.core.model.Place) : MapIntent
    data object SearchRoute : MapIntent
}
