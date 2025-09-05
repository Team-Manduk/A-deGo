package com.teammanduk.adego.feature.home.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.teammanduk.adego.core.navigation.Route
import com.teammanduk.adego.feature.home.HomeRoute

fun NavController.navigateToHome(
    navOptions: NavOptions? = null
) {
    navigate(Route.Home, navOptions)
}

fun NavGraphBuilder.homeNavGraph() {
    composable<Route.Home> {
        HomeRoute()
    }
}