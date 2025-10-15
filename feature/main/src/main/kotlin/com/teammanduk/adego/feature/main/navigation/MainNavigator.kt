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
) {
    private val currentDestination: NavDestination?
        @Composable get() = navController.currentBackStackEntryAsState().value?.destination
    val startDestination = Route.Home
    private val navOptions by lazy {
        navOptions {
            popUpTo(navController.graph.findStartDestination().id)
            launchSingleTop = true
        }
    }
    

    fun navigateHome() {
        navController.navigateToHome(navOptions)
    }

    fun navigateToCreate() {
        navController.navigateToCreate(navOptions)
    }

    fun navigateToSelectPlace() {
        navController.navigateToSelectPlace()
    }

    fun navigateToMap(roomId: String, userId: String) {
        navController.navigateToMap(roomId, userId, navOptions)
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
): MainNavigator = remember(navController) {
    MainNavigator(navController)
}