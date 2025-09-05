package com.teammanduk.adego.feature.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import com.teammanduk.adego.feature.home.navigation.homeNavGraph
import com.teammanduk.adego.feature.main.navigation.MainNavigator

@Composable
internal fun MainNavHost(
    navigator: MainNavigator,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        NavHost(
            navController = navigator.navController,
            startDestination = navigator.startDestination
        ) {
            homeNavGraph()
        }
    }
}