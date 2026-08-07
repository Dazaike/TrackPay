package com.trackpay.app.ui.shell

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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.trackpay.app.ui.dashboard.DashboardRoute
import com.trackpay.app.ui.goals.GoalsPlaceholder
import com.trackpay.app.ui.history.HistoryPlaceholder
import com.trackpay.app.ui.insights.InsightsPlaceholder
import com.trackpay.app.ui.jobs.JobEditorRoute
import com.trackpay.app.ui.jobs.JobsListRoute
import com.trackpay.app.ui.settings.SettingsScreen

private object Routes {
    const val JOBS = "jobs"
    const val JOB_EDIT = "jobEdit"
    const val JOB_EDIT_ARG = "jobId"
    const val JOB_EDIT_ROUTE = "jobEdit/{jobId}"
}

@Composable
fun TrackPayAppShell() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route
    val showBottomBar = currentRoute in TopLevelDestination.entries.map { it.route }.toSet()

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    TopLevelDestination.entries.forEach { destination ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == destination.route
                        } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) {
                                        destination.selectedIcon
                                    } else {
                                        destination.icon
                                    },
                                    contentDescription = destination.label,
                                )
                            },
                            label = { Text(destination.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopLevelDestination.Dashboard.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(TopLevelDestination.Dashboard.route) {
                DashboardRoute(
                    onOpenJobs = { navController.navigate(Routes.JOBS) },
                )
            }
            composable(TopLevelDestination.History.route) {
                HistoryPlaceholder()
            }
            composable(TopLevelDestination.Insights.route) {
                InsightsPlaceholder()
            }
            composable(TopLevelDestination.Goals.route) {
                GoalsPlaceholder()
            }
            composable(TopLevelDestination.Settings.route) {
                SettingsScreen(
                    onOpenJobs = { navController.navigate(Routes.JOBS) },
                )
            }
            composable(Routes.JOBS) {
                JobsListRoute(
                    onBack = { navController.popBackStack() },
                    onAddJob = { navController.navigate("${Routes.JOB_EDIT}/new") },
                    onEditJob = { id -> navController.navigate("${Routes.JOB_EDIT}/$id") },
                )
            }
            composable(
                route = Routes.JOB_EDIT_ROUTE,
                arguments = listOf(
                    navArgument(Routes.JOB_EDIT_ARG) { type = NavType.StringType },
                ),
            ) { entry ->
                val jobId = entry.arguments?.getString(Routes.JOB_EDIT_ARG)
                JobEditorRoute(
                    jobId = jobId?.takeUnless { it == "new" },
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
