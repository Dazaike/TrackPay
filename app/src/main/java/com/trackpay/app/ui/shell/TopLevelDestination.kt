package com.trackpay.app.ui.shell

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector

enum class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon,
) {
    Dashboard("dashboard", "Dashboard", Icons.Default.Home),
    History("history", "History", Icons.AutoMirrored.Filled.List),
    Insights("insights", "Insights", Icons.Default.Info),
    Goals("goals", "Goals", Icons.Default.Star),
    Settings("settings", "Settings", Icons.Default.Settings),
}
