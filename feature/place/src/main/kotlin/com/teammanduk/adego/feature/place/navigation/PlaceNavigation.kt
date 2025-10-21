package com.teammanduk.adego.feature.place.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.teammanduk.adego.core.navigation.Route
import com.teammanduk.adego.feature.place.SearchPlaceRoute
import com.teammanduk.adego.feature.place.SelectPlaceRoute

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

fun NavController.navigateToSelectStartPlace(
    roomId: String,
    userId: String,
    destLat: Double,
    destLng: Double,
    navOptions: NavOptions? = null
) {
    navigate(
        Route.SelectStartPlace(
            roomId = roomId,
            userId = userId,
            destLat = destLat,
            destLng = destLng
        ),
        navOptions
    )
}

fun NavGraphBuilder.placeNavGraph(
    navController: NavController,
    onNavigateBack: () -> Unit,
    onStartPlaceSelected: (String, String, Double, Double, Double, Double) -> Unit = { _, _, _, _, _, _ -> }
) {
    selectPlaceScreen(
        onBackClick = onNavigateBack,
        onSearchClick = { navController.navigateToSearchPlace() },
        onPlaceSelected = onNavigateBack
    )

    searchPlaceScreen(
        onBackClick = { navController.popBackStack() },
        onPlaceClick = { navController.popBackStack() }
    )

    selectStartPlaceScreen(
        navController = navController,
        onBackClick = onNavigateBack,
        onPlaceSelected = onStartPlaceSelected
    )
}

fun NavGraphBuilder.selectPlaceScreen(
    onBackClick: () -> Unit,
    onSearchClick: () -> Unit,
    onPlaceSelected: () -> Unit
) {
    composable<Route.SelectPlace> {
        SelectPlaceRoute(
            title = "모임 장소 선택",
            buttonText = "모임 장소 선택하기",
            showTopBar = true,
            onBackClick = onBackClick,
            onPlaceSelected = { _ -> onPlaceSelected() },
            onSearchClick = onSearchClick
        )
    }
}

fun NavGraphBuilder.searchPlaceScreen(
    onBackClick: () -> Unit,
    onPlaceClick: () -> Unit
) {
    composable<Route.SearchPlace> {
        SearchPlaceRoute(
            onBackClick = onBackClick,
            onPlaceClick = onPlaceClick
        )
    }
}

fun NavGraphBuilder.selectStartPlaceScreen(
    navController: NavController,
    onBackClick: () -> Unit,
    onPlaceSelected: (String, String, Double, Double, Double, Double) -> Unit
) {
    composable<Route.SelectStartPlace> { backStackEntry ->
        val args = backStackEntry.arguments?.let {
            Route.SelectStartPlace(
                roomId = it.getString("roomId") ?: "",
                userId = it.getString("userId") ?: "",
                destLat = it.getDouble("destLat"),
                destLng = it.getDouble("destLng")
            )
        } ?: return@composable

        SelectPlaceRoute(
            title = "출발지 선택",
            buttonText = "출발지 선택하기",
            showTopBar = true,
            clearPreviousResult = true,
            onBackClick = onBackClick,
            onPlaceSelected = { place ->
                onPlaceSelected(
                    args.roomId,
                    args.userId,
                    place.latitude,
                    place.longitude,
                    args.destLat,
                    args.destLng
                )
            },
            onSearchClick = { navController.navigateToSearchPlace() }
        )
    }
}
