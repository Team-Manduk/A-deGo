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

fun NavGraphBuilder.homeNavGraph(
    onNavigateToCreate: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToMap: (String, String) -> Unit,
) {
    composable<Route.Home> {
        HomeRoute(
            onNavigateToCreate = onNavigateToCreate,
            onNavigateToSettings = onNavigateToSettings,
            onNavigateToMap = onNavigateToMap,
        )
    }
}