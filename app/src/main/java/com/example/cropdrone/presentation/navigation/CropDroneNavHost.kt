package com.example.cropdrone.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.cropdrone.presentation.screens.dashboard.DashboardScreen
import com.example.cropdrone.presentation.screens.home.HomeScreen
import com.example.cropdrone.presentation.screens.map.MapScreen
import com.example.cropdrone.presentation.screens.mission.MissionScreen
import com.example.cropdrone.presentation.screens.settings.SettingsScreen

@Composable
fun CropDroneNavHost() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: Screen.Home.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                Screen.bottomNavItems.forEach { screen ->
                    val selected = currentRoute == screen.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                contentDescription = screen.label,
                            )
                        },
                        label = { Text(screen.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Screen.Home.route) { HomeScreen(navController) }
            composable(Screen.Map.route) { MapScreen(navController) }
            composable(Screen.Mission.route) { MissionScreen(navController) }
            composable(Screen.Dashboard.route) { DashboardScreen(navController) }
            composable(Screen.Settings.route) { SettingsScreen(navController) }
        }
    }
}
