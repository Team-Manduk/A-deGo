package com.teammanduk.adego.feature.create.navigation

import androidx.compose.runtime.collectAsState
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

fun NavGraphBuilder.createNavGraph(
    navController: NavController,
    onNavigateBack: () -> Unit,
    onNavigateToSelectPlace: () -> Unit,
    onCreateMeeting: (String, Int, Int) -> Unit,
) {
    composable<Route.Create> { backStackEntry ->
        val selectedPlace = navController.currentBackStackEntry
            ?.savedStateHandle
            ?.getStateFlow("selected_place", "")
            ?.collectAsState()

        CreateRoute(
            onNavigateBack = onNavigateBack,
            onNavigateToSelectPlace = onNavigateToSelectPlace,
            onCreateMeeting = onCreateMeeting,
            selectedPlaceFromNav = selectedPlace?.value,
        )
    }

    composable<Route.SelectPlace> {
        SelectPlaceRoute(
            onBackClick = onNavigateBack,
            onPlaceSelected = onNavigateBack
        )
    }
}
