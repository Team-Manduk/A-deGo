package com.teammanduk.adego.feature.route.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.teammanduk.adego.core.model.Route
import com.teammanduk.adego.core.navigation.Route as NavigationRoute
import com.teammanduk.adego.feature.route.RouteGuidanceRoute
import com.teammanduk.adego.feature.route.SelectRouteRoute

fun NavController.navigateToSelectRoute(
    roomId: String,
    userId: String,
    startLat: Double,
    startLng: Double,
    destLat: Double,
    destLng: Double,
    navOptions: NavOptions? = null
) {
    navigate(
        NavigationRoute.SelectRoute(
            roomId = roomId,
            userId = userId,
            startLat = startLat,
            startLng = startLng,
            destLat = destLat,
            destLng = destLng
        ),
        navOptions
    )
}

fun NavController.navigateToRouteGuidance(
    roomId: String,
    userId: String,
    navOptions: NavOptions? = null
) {
    navigate(
        NavigationRoute.RouteGuidance(
            roomId = roomId,
            userId = userId
        ),
        navOptions
    )
}

fun NavGraphBuilder.selectRouteNavGraph(
    onNavigateBack: () -> Unit,
    onRouteSelected: (Route) -> Unit
) {
    composable<NavigationRoute.SelectRoute> {
        SelectRouteRoute(
            onNavigateBack = onNavigateBack,
            onRouteSelected = onRouteSelected
        )
    }
}

fun NavGraphBuilder.routeGuidanceNavGraph(
    onNavigateBack: () -> Unit
) {
    composable<NavigationRoute.RouteGuidance> {
        RouteGuidanceRoute(
            onNavigateBack = onNavigateBack
        )
    }
}
