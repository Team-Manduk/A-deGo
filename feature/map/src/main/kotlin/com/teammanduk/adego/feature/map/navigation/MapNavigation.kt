package com.teammanduk.adego.feature.map.navigation

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.teammanduk.adego.core.navigation.Route
import com.teammanduk.adego.feature.map.MapRoute
import com.teammanduk.adego.feature.map.MapViewModel

fun NavController.navigateToMap(
    roomId: String,
    userId: String,
    hour: Int,
    minute: Int,
    navOptions: NavOptions? = null
) {
    navigate(Route.Map(roomId = roomId, userId = userId, hour = hour, minute = minute), navOptions)
}

fun NavGraphBuilder.mapNavGraph() {
    composable<Route.Map> { backStackEntry ->
        val route = backStackEntry.toRoute<Route.Map>()
        val meetingTime = String.format("%02d시 %02d분", route.hour, route.minute)

        val viewModel: MapViewModel = hiltViewModel<MapViewModel, MapViewModel.Factory> { factory ->
            factory.create(roomId = route.roomId, userId = route.userId)
        }

        MapRoute(
            roomId = route.roomId,
            meetingTime = meetingTime,
            viewModel = viewModel
        )
    }
}
