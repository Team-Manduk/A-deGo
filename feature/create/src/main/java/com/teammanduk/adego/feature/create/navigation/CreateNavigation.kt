package com.teammanduk.adego.feature.create.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.teammanduk.adego.core.navigation.Route
import com.teammanduk.adego.feature.create.CreateRoute
import com.teammanduk.adego.feature.create.SelectPlaceRoute

fun NavController.navigateToCreate(
    navOptions: NavOptions? = null
) {
    navigate(Route.Create, navOptions)
}

fun NavController.navigateToSelectPlace(
    navOptions: NavOptions? = null
) {
    navigate(Route.SelectPlace, navOptions)
}

fun NavGraphBuilder.createNavGraph() {
    composable<Route.Create> {
        CreateRoute()
    }

    composable<Route.SelectPlace> {
        SelectPlaceRoute()
    }
}
