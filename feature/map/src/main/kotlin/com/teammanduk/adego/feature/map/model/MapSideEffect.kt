package com.teammanduk.adego.feature.map.model

sealed interface MapSideEffect {
    data object NavigateToHome : MapSideEffect
}
