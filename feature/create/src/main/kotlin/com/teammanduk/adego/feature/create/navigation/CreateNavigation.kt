package com.teammanduk.adego.feature.create.navigation

import androidx.compose.runtime.collectAsState
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.teammanduk.adego.core.navigation.Route
import com.teammanduk.adego.feature.create.CreateRoute
import com.teammanduk.adego.feature.place.SearchPlaceRoute
import com.teammanduk.adego.feature.place.SelectPlaceRoute

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

fun NavController.navigateToSearchPlace(
    navOptions: NavOptions? = null
) {
    navigate(Route.SearchPlace, navOptions)
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

    composable<Route.SelectPlace> {
        SelectPlaceRoute(
            title = "모임 장소 선택",
            buttonText = "모임 장소 선택하기",
            showTopBar = true,
            onBackClick = onNavigateBack,
            onPlaceSelected = onNavigateBack,
            onSearchClick = {
                navController.navigateToSearchPlace()
            }
        )
    }

    composable<Route.SearchPlace> {
        SearchPlaceRoute(
            onBackClick = {
                navController.popBackStack()
            },
            onPlaceClick = {
                // 선택한 장소가 ViewModel에 저장되었으므로 SelectPlaceScreen으로 돌아가기
                navController.popBackStack()
            }
        )
    }
}
