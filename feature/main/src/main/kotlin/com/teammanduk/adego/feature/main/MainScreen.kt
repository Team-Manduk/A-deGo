package com.teammanduk.adego.feature.main

import androidx.compose.runtime.Composable
import com.teammanduk.adego.feature.main.navigation.MainNavigator

@Composable
internal fun MainRoute(
    navigator: MainNavigator
) {
    MainScreen(
        navigator = navigator
    )
}

@Composable
private fun MainScreen(
    navigator: MainNavigator
) {
    MainNavHost(
        navigator = navigator
    )
}