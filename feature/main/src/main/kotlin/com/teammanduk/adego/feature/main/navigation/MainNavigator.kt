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
import com.teammanduk.adego.feature.create.navigation.navigateToSelectPlace
import com.teammanduk.adego.feature.home.navigation.navigateToHome
import com.teammanduk.adego.feature.map.navigation.navigateToMap

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
        navController.navigateToSelectPlace(navOptions)
    }

    fun navigateToMap() {
        navController.navigateToMap(navOptions)
    }
}

@Composable
internal fun rememberMainNavigator(
    navController: NavHostController = rememberNavController(),
): MainNavigator = remember(navController) {
    MainNavigator(navController)
}