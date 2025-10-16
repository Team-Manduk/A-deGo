package com.teammanduk.adego.feature.map.model

sealed interface MapIntent {
    data object GenerateRandomName : MapIntent
    data class SelectParticipant(val index: Int) : MapIntent
    data object ClearError : MapIntent

    // 초대 다이얼로그
    data object ShowInviteDialog : MapIntent
    data object DismissInviteDialog : MapIntent

    // 경로 설정 다이얼로그
    data object ShowRouteDialog : MapIntent
    data object DismissRouteDialog : MapIntent

    // 출발지 선택
    data object ShowSelectStartPlace : MapIntent
    data object DismissSelectStartPlace : MapIntent
    data class StartPlaceSelected(val place: com.teammanduk.adego.core.model.Place) : MapIntent

    // 장소 검색
    data object ShowSearchPlace : MapIntent
    data object DismissSearchPlace : MapIntent

    // 경로 검색
    data object SearchRoute : MapIntent

    // 경로 선택
    data class SelectRoute(val routeIndex: Int) : MapIntent

    data class UpdateUserName(val userName: String) : MapIntent
    data object ConfirmUserName : MapIntent
}
