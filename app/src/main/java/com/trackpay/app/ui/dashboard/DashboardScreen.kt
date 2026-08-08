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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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

    val isDark = MaterialTheme.colorScheme.background == Color(0xFF1A1110)
    val surfaceBg = if (isDark) Color(0xFF1A1110) else MaterialTheme.colorScheme.background

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                        )
                        Text(
                            text = "Dashboard",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { menuOpen = true },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF271D1C) else MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false },
                        modifier = Modifier.background(if (isDark) Color(0xFF322726) else MaterialTheme.colorScheme.surfaceContainer),
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.dashboard_jobs), color = MaterialTheme.colorScheme.onSurface) },
                            onClick = {
                                menuOpen = false
                                onOpenJobs()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Work, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                            },
                        )
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = surfaceBg,
                ),
            )
        },
        containerColor = surfaceBg,
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
    val isDark = MaterialTheme.colorScheme.background == Color(0xFF1A1110)
    val cardBg = if (isDark) Color(0xFF271D1C) else MaterialTheme.colorScheme.surfaceVariant
    val overlayBg = if (isDark) Color(0xFF322726) else MaterialTheme.colorScheme.surfaceContainerHigh

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Dark summary card with streak pill & top goal overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = if (state.topGoal != null) 36.dp else 0.dp),
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
            ) {
                Column(
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = if (state.topGoal != null) 44.dp else 20.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column {
                            Text(
                                text = state.selectedJob?.name ?: "No job",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            state.selectedJob?.let { job ->
                                Text(
                                    text = formatMoneyRate(job.hourlyRateMinor),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        if (state.streakCurrentDays >= 1) {
                            val infiniteTransition = rememberInfiniteTransition(label = "streakPulsing")
                            val streakScale by infiniteTransition.animateFloat(
                                initialValue = 0.95f,
                                targetValue = 1.18f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1400, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse,
                                ),
                                label = "streakScale",
                            )
                            Box(
                                modifier = Modifier
                                    .graphicsLayer {
                                        scaleX = streakScale
                                        scaleY = streakScale
                                    }
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF5C4400))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                            ) {
                                Text(
                                    text = "🔥 ${state.streakCurrentDays}d",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFFFDEA6),
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Column {
                            Text(
                                text = "TODAY",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 0.5.sp,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = formatMoney(state.todayEarnedMinor),
                                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 32.sp),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                            )
                        }
                        Column(modifier = Modifier.padding(bottom = 4.dp)) {
                            Text(
                                text = "WEEK",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 0.5.sp,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = formatMoney(state.weekEarnedMinor),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }

            state.topGoal?.let { goal ->
                val rawTarget = goal.progress.coerceIn(0f, 1f)
                val animatedProgress by animateFloatAsState(
                    targetValue = rawTarget,
                    animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                    label = "goalProgress",
                )
                val percent = (animatedProgress * 100f).roundToInt().coerceIn(0, 100)
                val goalColor = Color(goal.goal.colorArgb)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp)
                        .layout { measurable, constraints ->
                            val placeable = measurable.measure(constraints)
                            layout(placeable.width, placeable.height) {
                                // Placed significantly lower down over card bottom
                                placeable.placeRelative(0, (placeable.height * 0.65f).roundToInt())
                            }
                        }
                        .clip(RoundedCornerShape(20.dp))
                        .background(overlayBg)
                        .clickable { onOpenGoal(goal.goal.id) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = goal.goal.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = goalColor,
                                trackColor = Color.White.copy(alpha = 0.12f),
                                strokeCap = StrokeCap.Round,
                            )
                        }
                        Text(
                            text = "$percent%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = goalColor,
                        )
                    }
                }
            }
        }

        // Job multi-picker chips
        if (state.jobs.size > 1) {
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.jobs.forEach { job ->
                    val isSel = state.selectedJob?.id == job.id
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isSel) MaterialTheme.colorScheme.primary else overlayBg)
                            .clickable { onSelectJob(job.id) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    ) {
                        Text(
                            text = job.name,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        // Clock in dial button
        val clockInCd = stringResource(R.string.a11y_clock_in)
        Button(
            onClick = onClockIn,
            modifier = Modifier
                .size(120.dp)
                .semantics { contentDescription = clockInCd },
            enabled = state.selectedJob != null,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.action_clock_in),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(Modifier.height(8.dp))

        TextButton(onClick = onOpenJobs) {
            Text(
                text = stringResource(R.string.dashboard_jobs),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
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
    val isDark = MaterialTheme.colorScheme.background == Color(0xFF1A1110)
    val statusColor = if (state.isPaused) Color(0xFFE0C38C) else MaterialTheme.colorScheme.primary
    val ringBg = if (isDark) Color(0xFF322726) else MaterialTheme.colorScheme.surfaceVariant
    val activeJob = active.job
    val startLabel = TimeFormat.formatSince(active.session.startAt)

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(
            modifier = Modifier
                .padding(top = 16.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            MoneyText(
                amountMinor = state.earnedMinor,
                color = statusColor,
            )
            Text(
                text = TimeFormat.formatElapsed(state.activeMillis),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFeatureSettings = "tnum",
                    fontWeight = FontWeight.Medium,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (state.isPaused) {
                Text(
                    text = stringResource(R.string.notif_status_paused),
                    color = statusColor,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(
                R.string.dashboard_rate_since,
                formatMoneyRate(active.session.snapshotHourlyRateMinor),
                startLabel,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            text = activeJob.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(Modifier.height(16.dp))

        // Controls row with pause/resume round button + large clock out button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            val pauseCd = stringResource(R.string.a11y_pause)
            val resumeCd = stringResource(R.string.a11y_resume)
            val clockOutCd = stringResource(R.string.a11y_clock_out)

            if (state.isPaused) {
                IconButton(
                    onClick = onResume,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF1F4230) else MaterialTheme.colorScheme.secondaryContainer)
                        .semantics { contentDescription = resumeCd },
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = if (isDark) Color(0xFFC8F0D9) else MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(24.dp),
                    )
                }
            } else {
                IconButton(
                    onClick = onPause,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF1F4230) else MaterialTheme.colorScheme.secondaryContainer)
                        .semantics { contentDescription = pauseCd },
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Pause,
                        contentDescription = null,
                        tint = if (isDark) Color(0xFFC8F0D9) else MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            Button(
                onClick = onClockOut,
                modifier = Modifier
                    .size(88.dp)
                    .semantics { contentDescription = clockOutCd },
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
            ) {
                Text(
                    text = stringResource(R.string.action_clock_out),
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
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
