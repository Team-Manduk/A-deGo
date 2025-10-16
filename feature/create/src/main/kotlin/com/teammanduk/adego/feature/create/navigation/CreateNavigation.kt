package com.teammanduk.adego.feature.create.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.teammanduk.adego.core.navigation.Route
import com.teammanduk.adego.feature.create.CreateRoute

fun NavController.navigateToCreate(
    navOptions: NavOptions? = null
) {
    navigate(Route.Create, navOptions)
}

fun NavGraphBuilder.createNavGraph(
    onNavigateBack: () -> Unit,
    onNavigateToSelectPlace: () -> Unit,
    onNavigateToMap: (String, String) -> Unit,
) {
    composable<Route.Create> {
        CreateRoute(
            onNavigateBack = onNavigateBack,
            onNavigateToSelectPlace = onNavigateToSelectPlace,
            onNavigateToMap = onNavigateToMap
        )
    }
}
