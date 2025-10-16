package com.teammanduk.adego.feature.create.model

sealed interface CreateSideEffect {
    data object NavigateToSelectPlace : CreateSideEffect
    data object NavigateBack : CreateSideEffect
    data class NavigateToMap(val roomId: String, val userId: String) : CreateSideEffect
}
