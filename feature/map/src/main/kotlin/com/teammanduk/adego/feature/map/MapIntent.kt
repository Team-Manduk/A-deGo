package com.teammanduk.adego.feature.map

/**
 * 지도 화면의 사용자 액션 (Intent)
 */
sealed interface MapIntent {
    // 참가자 선택
    data class SelectParticipant(val index: Int) : MapIntent

    // 에러 메시지 클리어
    data object ClearError : MapIntent

    // 초대 다이얼로그 표시/닫기
    data object ShowInviteDialog : MapIntent
    data object DismissInviteDialog : MapIntent
}
