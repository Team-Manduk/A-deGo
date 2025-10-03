package com.teammanduk.adego.feature.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import com.teammanduk.adego.feature.create.navigation.createNavGraph
import com.teammanduk.adego.feature.home.navigation.homeNavGraph
import com.teammanduk.adego.feature.main.navigation.MainNavigator
import com.teammanduk.adego.feature.map.navigation.mapNavGraph

@Composable
internal fun MainNavHost(
    navigator: MainNavigator,
) {
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
                onJoinWithCode = { code ->
                    /* TODO: 초대 코드로 입장 구현 */
                }
            )
            createNavGraph(
                onNavigateBack = { navigator.navController.navigateUp() },
                onNavigateToSelectPlace = navigator::navigateToSelectPlace,
                onCreateMeeting = { place, hour, minute ->
                    /* TODO: 모임 생성 로직 구현 */
                    navigator.navController.navigateUp()
                }
            )
            mapNavGraph()
        }
    }
}