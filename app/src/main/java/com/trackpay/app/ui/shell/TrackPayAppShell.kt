package com.trackpay.app.ui.shell

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.trackpay.app.domain.model.GoalDefaults
import com.trackpay.app.domain.usecase.ObserveOnboardingDoneUseCase
import com.trackpay.app.ui.dashboard.DashboardRoute
import com.trackpay.app.ui.goals.GoalEditorRoute
import com.trackpay.app.ui.goals.GoalsRoute
import com.trackpay.app.ui.history.HistoryRoute
import com.trackpay.app.ui.insights.InsightsRoute
import com.trackpay.app.ui.jobs.JobEditorRoute
import com.trackpay.app.ui.jobs.JobsListRoute
import com.trackpay.app.ui.onboarding.OnboardingRoute
import com.trackpay.app.ui.session.SessionDetailRoute
import com.trackpay.app.ui.session.SessionEditorRoute
import com.trackpay.app.ui.settings.AboutScreen
import com.trackpay.app.ui.settings.PrivacyScreen
import com.trackpay.app.ui.settings.SettingsRoute
import com.trackpay.app.ui.themes.ThemesRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

private object Routes {
    const val ONBOARDING = "onboarding"
    const val JOBS = "jobs"
    const val JOB_EDIT = "jobEdit"
    const val JOB_EDIT_ARG = "jobId"
    const val JOB_EDIT_ROUTE = "jobEdit/{jobId}"
    const val SESSION_DETAIL = "sessionDetail"
    const val SESSION_DETAIL_ARG = "sessionId"
    const val SESSION_DETAIL_ROUTE = "sessionDetail/{sessionId}"
    const val SESSION_EDIT = "sessionEdit"
    const val SESSION_EDIT_ARG = "sessionId"
    const val SESSION_EDIT_ROUTE = "sessionEdit/{sessionId}"
    const val GOAL_EDIT = "goalEdit"
    const val GOAL_EDIT_ARG = "goalId"
    const val GOAL_EDIT_TEMPLATE_ARG = "template"
    const val GOAL_EDIT_ROUTE = "goalEdit/{goalId}?template={template}"
    const val THEMES = "themes"
    const val PRIVACY = "privacy"
    const val ABOUT = "about"
}

data class ShellNavState(
    val ready: Boolean = false,
    val onboardingDone: Boolean = false,
)

@HiltViewModel
class ShellViewModel @Inject constructor(
    observeOnboardingDone: ObserveOnboardingDoneUseCase,
) : ViewModel() {
    val navState: StateFlow<ShellNavState> = observeOnboardingDone()
        .map { done -> ShellNavState(ready = true, onboardingDone = done) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ShellNavState(),
        )
}

@Composable
fun TrackPayAppShell(
    viewModel: ShellViewModel = hiltViewModel(),
) {
    val navState by viewModel.navState.collectAsStateWithLifecycle()
    if (!navState.ready) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val startDestination = if (navState.onboardingDone) {
        TopLevelDestination.Dashboard.route
    } else {
        Routes.ONBOARDING
    }
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
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.ONBOARDING) {
                OnboardingRoute(
                    onFinished = {
                        navController.navigate(TopLevelDestination.Dashboard.route) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(TopLevelDestination.Dashboard.route) {
                DashboardRoute(
                    onOpenJobs = { navController.navigate(Routes.JOBS) },
                    onOpenGoal = { id ->
                        navController.navigate(goalEditRoute(id))
                    },
                )
            }
            composable(TopLevelDestination.History.route) {
                HistoryRoute(
                    onOpenSession = { id ->
                        navController.navigate("${Routes.SESSION_DETAIL}/$id")
                    },
                    onCreateSession = {
                        navController.navigate("${Routes.SESSION_EDIT}/new")
                    },
                )
            }
            composable(TopLevelDestination.Insights.route) {
                InsightsRoute()
            }
            composable(TopLevelDestination.Goals.route) {
                GoalsRoute(
                    onAddGoal = {
                        navController.navigate(goalEditRoute("new"))
                    },
                    onEditGoal = { id ->
                        navController.navigate(goalEditRoute(id))
                    },
                    onUseTemplate = { template ->
                        navController.navigate(goalEditRoute("new", template.name))
                    },
                )
            }
            composable(TopLevelDestination.Settings.route) {
                SettingsRoute(
                    onOpenJobs = { navController.navigate(Routes.JOBS) },
                    onOpenThemes = { navController.navigate(Routes.THEMES) },
                    onOpenPrivacy = { navController.navigate(Routes.PRIVACY) },
                    onOpenAbout = { navController.navigate(Routes.ABOUT) },
                )
            }
            composable(Routes.THEMES) {
                ThemesRoute(
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.PRIVACY) {
                PrivacyScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.ABOUT) {
                AboutScreen(onBack = { navController.popBackStack() })
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
            composable(
                route = Routes.GOAL_EDIT_ROUTE,
                arguments = listOf(
                    navArgument(Routes.GOAL_EDIT_ARG) { type = NavType.StringType },
                    navArgument(Routes.GOAL_EDIT_TEMPLATE_ARG) {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                ),
            ) { entry ->
                val rawId = entry.arguments?.getString(Routes.GOAL_EDIT_ARG)
                val templateName = entry.arguments
                    ?.getString(Routes.GOAL_EDIT_TEMPLATE_ARG)
                    .orEmpty()
                    .let { raw -> if (raw.isBlank()) null else Uri.decode(raw) }
                val template = templateName?.let { name ->
                    GoalDefaults.TEMPLATES.firstOrNull { it.name == name }
                }
                GoalEditorRoute(
                    goalId = rawId?.takeUnless { it == "new" },
                    template = template,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = Routes.SESSION_DETAIL_ROUTE,
                arguments = listOf(
                    navArgument(Routes.SESSION_DETAIL_ARG) { type = NavType.StringType },
                ),
            ) { entry ->
                val sessionId = entry.arguments?.getString(Routes.SESSION_DETAIL_ARG).orEmpty()
                SessionDetailRoute(
                    sessionId = sessionId,
                    onBack = { navController.popBackStack() },
                    onEdit = { id -> navController.navigate("${Routes.SESSION_EDIT}/$id") },
                )
            }
            composable(
                route = Routes.SESSION_EDIT_ROUTE,
                arguments = listOf(
                    navArgument(Routes.SESSION_EDIT_ARG) { type = NavType.StringType },
                ),
            ) { entry ->
                val rawId = entry.arguments?.getString(Routes.SESSION_EDIT_ARG)
                SessionEditorRoute(
                    sessionId = rawId?.takeUnless { it == "new" },
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

private fun goalEditRoute(goalId: String, templateName: String? = null): String {
    val encodedTemplate = Uri.encode(templateName.orEmpty())
    return "${Routes.GOAL_EDIT}/$goalId?template=$encodedTemplate"
}
