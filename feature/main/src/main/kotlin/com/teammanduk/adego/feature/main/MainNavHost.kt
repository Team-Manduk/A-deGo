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
                    // TODO: 초대 코드로 서버에서 모임 정보 조회 후 시간 정보 가져오기
                    // 임시: 기본 시간(0시 0분)으로 Map 화면 이동
                    navigator.navigateToMap(0, 0)
                }
            )
            createNavGraph(
                navController = navigator.navController,
                onNavigateBack = navigator::navigateBack,
                onNavigateToSelectPlace = navigator::navigateToSelectPlace,
                onCreateMeeting = { place, hour, minute ->
                    /* TODO: 모임 생성 로직 구현 */
                    navigator.navigateToMap(hour, minute)
                }
            )
            mapNavGraph()
        }
    }
}