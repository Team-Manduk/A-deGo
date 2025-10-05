package com.teammanduk.adego.feature.map.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.teammanduk.adego.core.navigation.Route
import com.teammanduk.adego.feature.map.MapRoute

fun NavController.navigateToMap(
    hour: Int,
    minute: Int,
    navOptions: NavOptions? = null
) {
    navigate(Route.Map(hour = hour, minute = minute), navOptions)
}

fun NavGraphBuilder.mapNavGraph() {
    composable<Route.Map> { backStackEntry ->
        val route = backStackEntry.toRoute<Route.Map>()
        val meetingTime = String.format("%02d시 %02d분", route.hour, route.minute)

        MapRoute(
            meetingTime = meetingTime
        )
    }
}
