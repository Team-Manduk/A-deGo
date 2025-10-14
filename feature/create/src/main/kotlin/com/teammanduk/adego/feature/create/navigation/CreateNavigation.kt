package com.teammanduk.adego.feature.create.navigation

import androidx.compose.runtime.collectAsState
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
    navController: NavController,
    onNavigateBack: () -> Unit,
    onNavigateToSelectPlace: () -> Unit,
    onCreateMeeting: (String, String) -> Unit,
) {
    composable<Route.Create> { backStackEntry ->
        val selectedPlace = navController.currentBackStackEntry
            ?.savedStateHandle
            ?.getStateFlow("selected_place", "")
            ?.collectAsState()

        CreateRoute(
            onNavigateBack = onNavigateBack,
            onNavigateToSelectPlace = onNavigateToSelectPlace,
            onNavigateToMap = { roomId, userId ->
                onCreateMeeting(roomId, userId)
            },
            selectedPlaceFromNav = selectedPlace?.value,
        )
    }
}
