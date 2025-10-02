package com.teammanduk.adego.feature.map.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.teammanduk.adego.core.navigation.Route
import com.teammanduk.adego.feature.map.MapRoute

fun NavController.navigateToMap(
    navOptions: NavOptions? = null
) {
    navigate(Route.Map, navOptions)
}

fun NavGraphBuilder.mapNavGraph() {
    composable<Route.Map> {
        MapRoute()
    }
}
