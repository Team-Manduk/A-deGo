package com.teammanduk.adego.feature.main.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import com.teammanduk.adego.core.navigation.Route
import com.teammanduk.adego.feature.create.navigation.navigateToCreate
import com.teammanduk.adego.feature.home.navigation.navigateToHome
import com.teammanduk.adego.feature.map.navigation.navigateToMap
import com.teammanduk.adego.feature.place.navigation.navigateToSelectPlace
import com.teammanduk.adego.feature.place.navigation.navigateToSelectStartPlace
import com.teammanduk.adego.feature.route.navigation.navigateToSelectRoute

internal class MainNavigator(
    val navController: NavHostController,
    val startDestination: Route = Route.Home
) {
    private val currentDestination: NavDestination?
        @Composable get() = navController.currentBackStackEntryAsState().value?.destination
    private val navOptions by lazy {
        navOptions {
            popUpTo(navController.graph.findStartDestination().id)
            launchSingleTop = true
        }
    }
    

    /**
     * Home 화면으로 이동 (모든 백스택 제거)
     *
     * 방 나가기 등으로 Home으로 돌아갈 때 사용.
     * 백스택의 모든 화면을 제거하고 Home만 남김.
     */
    fun navigateHome() {
        val homeNavOptions = navOptions {
            // 백스택의 모든 화면 제거
            popUpTo(0) { inclusive = true }
            launchSingleTop = true
        }
        navController.navigateToHome(homeNavOptions)
    }

    fun navigateToCreate() {
        navController.navigateToCreate()
    }

    fun navigateToSelectPlace() {
        navController.navigateToSelectPlace()
    }

    /**
     * Map 화면으로 이동 (세션 시작)
     *
     * Home 위의 모든 화면을 제거하고 Map을 최상위로 배치.
     * 백버튼으로는 Home으로 이동할 수 없으며, 앱 종료만 가능.
     *
     * @param userName 세션 복구 시 자동으로 설정할 사용자 이름 (선택)
     *
     * TODO: 추후 "일시 나가기" 기능 추가 시
     *  - navigateHomeTemporarily() 함수 추가
     *  - navOptions 없이 단순 백스택 이동만 수행
     */
    fun navigateToMap(roomId: String, userId: String, userName: String? = null) {
        val mapNavOptions = navOptions {
            // Home 위의 모든 화면 제거
            popUpTo(Route.Home) {
                inclusive = false  // Home은 백스택에 유지
            }
            launchSingleTop = true
        }
        navController.navigateToMap(roomId, userId, userName, mapNavOptions)
    }

    fun navigateToSelectStartPlace(
        roomId: String,
        userId: String,
        destLat: Double,
        destLng: Double
    ) {
        navController.navigateToSelectStartPlace(roomId, userId, destLat, destLng)
    }

    fun navigateToSelectRoute(
        roomId: String,
        userId: String,
        startLat: Double,
        startLng: Double,
        destLat: Double,
        destLng: Double
    ) {
        navController.navigateToSelectRoute(roomId, userId, startLat, startLng, destLat, destLng)
    }

    fun navigateBack() {
        navController.navigateUp()
    }
}

@Composable
internal fun rememberMainNavigator(
    navController: NavHostController = rememberNavController(),
    startDestination: Route = Route.Home
): MainNavigator = remember(navController, startDestination) {
    MainNavigator(navController, startDestination)
}