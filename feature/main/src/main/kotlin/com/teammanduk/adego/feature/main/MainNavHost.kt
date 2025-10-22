package com.teammanduk.adego.feature.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import com.teammanduk.adego.core.navigation.Route
import com.teammanduk.adego.feature.create.navigation.createNavGraph
import com.teammanduk.adego.feature.home.navigation.homeNavGraph
import com.teammanduk.adego.feature.main.navigation.MainNavigator
import com.teammanduk.adego.feature.map.navigation.mapNavGraph
import com.teammanduk.adego.feature.place.navigation.placeNavGraph
import com.teammanduk.adego.feature.route.navigation.routeGuidanceNavGraph
import com.teammanduk.adego.feature.route.navigation.selectRouteNavGraph

@Composable
internal fun MainNavHost(
    navigator: MainNavigator,
) {
    // selectedRoute는 Room DB에 저장되므로 상태 관리 불필요

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        NavHost(
            navController = navigator.navController,
            startDestination = navigator.startDestination
        ) {
            homeNavGraph(
                onNavigateToCreate = navigator::navigateToCreate,
                onNavigateToSettings = { /* TODO: 설정 화면 구현 */ },
                onNavigateToMap = navigator::navigateToMap
            )
            createNavGraph(
                onNavigateBack = navigator::navigateBack,
                onNavigateToSelectPlace = navigator::navigateToSelectPlace,
                onNavigateToMap = navigator::navigateToMap
            )
            placeNavGraph(
                navController = navigator.navController,
                onNavigateBack = navigator::navigateBack,
                onStartPlaceSelected = navigator::navigateToSelectRoute
            )
            mapNavGraph(
                onNavigateToSelectStartPlace = navigator::navigateToSelectStartPlace,
                onNavigateToHome = navigator::navigateHome,
                onNavigateToRouteGuidance = navigator::navigateToRouteGuidance
            )
            selectRouteNavGraph(
                onNavigateBack = navigator::navigateBack,
                onRouteSelected = {
                    // 경로는 SaveSelectedRouteUseCase에서 Room DB에 저장됨
                    // 경로 선택 화면과 출발지 선택 화면을 모두 pop하고 MapScreen으로 돌아가기
                    navigator.navController.popBackStack(
                        route = Route.Map::class,
                        inclusive = false
                    )
                }
            )
            routeGuidanceNavGraph(
                onNavigateBack = navigator::navigateBack
            )
        }
    }
}