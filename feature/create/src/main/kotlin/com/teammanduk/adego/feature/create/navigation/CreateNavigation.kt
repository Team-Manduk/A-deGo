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
            onPlaceSelected = {
                // TODO: ViewModel/Repository 구현 시
                // - CreateViewModel의 StateFlow에 선택된 장소 정보 업데이트
                // - CreateScreen에서 StateFlow를 관찰하여 자동으로 UI 업데이트
                // 예: viewModel.updateSelectedPlace(placeName, placeAddress)

                // 임시: 이전 화면(Create)의 savedStateHandle에 장소 정보 저장 (테스트용)
                navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.set("selected_place", "부산 북구 만덕대로 291")
                onNavigateBack()
            }
        )
    }
}
