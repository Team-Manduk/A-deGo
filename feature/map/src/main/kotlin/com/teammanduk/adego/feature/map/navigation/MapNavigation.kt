package com.teammanduk.adego.feature.map.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.teammanduk.adego.core.navigation.Route
import com.teammanduk.adego.feature.map.MapRoute

fun NavController.navigateToMap(
    roomId: String,
    userId: String,
    navOptions: NavOptions? = null
) {
    navigate(Route.Map(roomId = roomId, userId = userId), navOptions)
}

fun NavGraphBuilder.mapNavGraph(
    onNavigateToSelectStartPlace: (String, String, Double, Double) -> Unit = { _, _, _, _ -> },
    getSelectedRoute: () -> com.teammanduk.adego.core.model.Route? = { null }
) {
    composable<Route.Map> {
        MapRoute(
            onNavigateToSelectStartPlace = onNavigateToSelectStartPlace,
            selectedRoute = getSelectedRoute()
        )
    }
}
