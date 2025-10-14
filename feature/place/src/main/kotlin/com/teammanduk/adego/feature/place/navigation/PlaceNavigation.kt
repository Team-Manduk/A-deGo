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
            onPlaceSelected = onPlaceSelected,
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
