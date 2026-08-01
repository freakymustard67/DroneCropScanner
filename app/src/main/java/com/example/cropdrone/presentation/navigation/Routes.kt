package com.example.cropdrone.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class Screen(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    Home("home", "Home", Icons.Filled.Home, Icons.Outlined.Home),
    Map("map", "Field", Icons.Filled.Map, Icons.Outlined.Map),
    Mission("mission", "Mission", Icons.Filled.Notifications, Icons.Outlined.Notifications),
    Dashboard("dashboard", "Results", Icons.Filled.Notifications, Icons.Outlined.Notifications),
    Settings("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
    ;

    companion object {
        val bottomNavItems = listOf(Home, Map, Mission, Dashboard, Settings)
    }
}
