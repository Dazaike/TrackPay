package com.trackpay.app.ui.dashboard

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trackpay.app.R
import com.trackpay.app.domain.model.GoalProgress
import com.trackpay.app.domain.model.Job
import com.trackpay.app.ui.components.MoneyText
import com.trackpay.app.ui.util.TimeFormat
import kotlin.math.roundToInt
import com.trackpay.app.ui.util.formatMoney
import com.trackpay.app.ui.util.formatMoneyRate

@Composable
fun DashboardRoute(
    onOpenJobs: () -> Unit,
    onOpenGoal: (String) -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DashboardScreen(
        state = state,
        onOpenJobs = onOpenJobs,
        onOpenGoal = onOpenGoal,
        onSelectJob = viewModel::selectJob,
        onClockIn = viewModel::onClockIn,
        onPause = viewModel::onPause,
        onResume = viewModel::onResume,
        onClockOut = viewModel::onClockOut,
        onDismissError = viewModel::dismissError,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onOpenJobs: () -> Unit,
    onOpenGoal: (String) -> Unit = {},
    onSelectJob: (String) -> Unit,
    onClockIn: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onClockOut: () -> Unit,
    onDismissError: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var menuOpen by remember { mutableStateOf(false) }
    var jobMenuOpen by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* session runs even if denied */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(state.errorMessage) {
        val msg = state.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        onDismissError()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard") },
                actions = {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.dashboard_jobs)) },
                            onClick = {
                                menuOpen = false
                                onOpenJobs()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Work, contentDescription = null)
                            },
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when {
            state.loading -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Loading…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            state.jobs.isEmpty() -> {
                EmptyJobsPane(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    onCreateJob = onOpenJobs,
                )
            }

            state.hasActive -> {
                ActiveSessionPane(
                    state = state,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    onPause = onPause,
                    onResume = onResume,
                    onClockOut = onClockOut,
                )
            }

            else -> {
                IdlePane(
                    state = state,
                    jobMenuOpen = jobMenuOpen,
                    onJobMenuOpenChange = { jobMenuOpen = it },
                    onSelectJob = onSelectJob,
                    onClockIn = onClockIn,
                    onOpenJobs = onOpenJobs,
                    onOpenGoal = onOpenGoal,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                )
            }
        }
    }
}

@Composable
private fun EmptyJobsPane(
    onCreateJob: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.dashboard_no_jobs_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.dashboard_no_jobs_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onCreateJob) {
            Text(stringResource(R.string.dashboard_create_job))
        }
    }
}

@Composable
private fun IdlePane(
    state: DashboardUiState,
    jobMenuOpen: Boolean,
    onJobMenuOpenChange: (Boolean) -> Unit,
    onSelectJob: (String) -> Unit,
    onClockIn: () -> Unit,
    onOpenJobs: () -> Unit,
    onOpenGoal: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        PeriodChips(
            today = state.todayEarnedMinor,
            week = state.weekEarnedMinor,
            streakDays = state.streakCurrentDays,
        )

        state.topGoal?.let { goal ->
            DashboardGoalPeek(
                progress = goal,
                onClick = { onOpenGoal(goal.goal.id) },
            )
        }

        if (state.jobs.size > 1) {
            JobSelector(
                jobs = state.jobs,
                selected = state.selectedJob,
                expanded = jobMenuOpen,
                onExpandedChange = onJobMenuOpenChange,
                onSelect = onSelectJob,
            )
        } else {
            state.selectedJob?.let { job ->
                Text(
                    text = job.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = formatMoneyRate(job.hourlyRateMinor),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        val clockInCd = stringResource(R.string.a11y_clock_in)
        Button(
            onClick = onClockIn,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .semantics { contentDescription = clockInCd },
            enabled = state.selectedJob != null,
        ) {
            Text(
                text = stringResource(R.string.action_clock_in),
                style = MaterialTheme.typography.titleMedium,
            )
        }

        TextButton(onClick = onOpenJobs) {
            Text(stringResource(R.string.dashboard_jobs))
        }
    }
}

@Composable
private fun DashboardGoalPeek(
    progress: GoalProgress,
    onClick: () -> Unit,
) {
    val goal = progress.goal
    val color = Color(goal.colorArgb)
    val percent = (progress.progress * 100f).roundToInt().coerceIn(0, 100)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.dashboard_goal_peek_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = color,
                )
            }
            Text(
                text = goal.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            LinearProgressIndicator(
                progress = { progress.progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = color,
                trackColor = color.copy(alpha = 0.15f),
                strokeCap = StrokeCap.Round,
            )
            Text(
                text = stringResource(
                    R.string.dashboard_goal_peek_progress,
                    formatMoney(progress.savedMinor),
                    formatMoney(goal.targetMinor),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ActiveSessionPane(
    state: DashboardUiState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onClockOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val active = state.active ?: return
    val statusColor = if (state.isPaused) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.primary
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PeriodChips(today = state.todayEarnedMinor, week = state.weekEarnedMinor, streakDays = state.streakCurrentDays)

        Spacer(Modifier.height(8.dp))

        MoneyText(
            amountMinor = state.earnedMinor,
            color = statusColor,
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(statusColor),
            )
            Text(
                text = TimeFormat.formatElapsed(state.activeMillis),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFeatureSettings = "tnum",
                    fontWeight = FontWeight.Medium,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        Text(
            text = stringResource(
                R.string.dashboard_rate_since,
                formatMoneyRate(active.session.snapshotHourlyRateMinor),
                TimeFormat.formatSince(active.session.startAt),
            ),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            text = active.job.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        if (state.isPaused) {
            Text(
                text = stringResource(R.string.notif_status_paused),
                color = MaterialTheme.colorScheme.tertiary,
                style = MaterialTheme.typography.labelLarge,
            )
        }

        Spacer(Modifier.height(24.dp))

        val clockOutCd = stringResource(R.string.a11y_clock_out)
        val pauseCd = stringResource(R.string.a11y_pause)
        val resumeCd = stringResource(R.string.a11y_resume)
        Button(
            onClick = onClockOut,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .semantics { contentDescription = clockOutCd },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
        ) {
            Text(
                text = stringResource(R.string.action_clock_out),
                style = MaterialTheme.typography.titleMedium,
            )
        }

        if (state.isPaused) {
            FilledTonalButton(
                onClick = onResume,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .semantics { contentDescription = resumeCd },
            ) {
                Text(stringResource(R.string.action_resume))
            }
        } else {
            FilledTonalButton(
                onClick = onPause,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .semantics { contentDescription = pauseCd },
            ) {
                Text(stringResource(R.string.action_pause))
            }
        }
    }
}

@Composable
private fun PeriodChips(today: Long, week: Long, streakDays: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AssistChip(
            onClick = {},
            label = {
                Text("${stringResource(R.string.dashboard_today)} ${formatMoney(today)}")
            },
        )
        AssistChip(
            onClick = {},
            label = {
                Text("${stringResource(R.string.dashboard_week)} ${formatMoney(week)}")
            },
        )
        if (streakDays >= 1) {
            AssistChip(
                onClick = {},
                label = {
                    Text(stringResource(R.string.dashboard_streak_chip, streakDays))
                },
            )
        }
    }
}

@Composable
private fun JobSelector(
    jobs: List<Job>,
    selected: Job?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.dashboard_select_job),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box {
            FilledTonalButton(onClick = { onExpandedChange(true) }) {
                Text(selected?.name ?: "Select job")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
                jobs.forEach { job ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(job.name)
                                Text(
                                    formatMoneyRate(job.hourlyRateMinor),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                        onClick = {
                            onExpandedChange(false)
                            onSelect(job.id)
                        },
                    )
                }
            }
        }
    }
}
