package com.trackpay.app.ui.session

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.trackpay.app.R
import com.trackpay.app.domain.model.BreakInterval
import com.trackpay.app.domain.model.SessionDetail
import com.trackpay.app.domain.model.SessionStatus
import com.trackpay.app.ui.components.MoneyText
import com.trackpay.app.ui.util.DateTimeFormat
import java.util.Locale
import com.trackpay.app.ui.util.formatMoney
import com.trackpay.app.ui.util.formatMoneyRate

@Composable
fun SessionDetailRoute(
    sessionId: String,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    viewModel: SessionDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(sessionId, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.load(sessionId)
        }
    }
    LaunchedEffect(state.deleted) {
        if (state.deleted) onBack()
    }
    SessionDetailScreen(
        state = state,
        onBack = onBack,
        onEdit = { onEdit(sessionId) },
        onRequestDelete = viewModel::requestDelete,
        onDismissDelete = viewModel::dismissDelete,
        onConfirmDelete = viewModel::confirmDelete,
        onDismissError = viewModel::dismissError,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    state: SessionDetailUiState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onRequestDelete: () -> Unit,
    onDismissDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissError: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.errorMessage) {
        val msg = state.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        onDismissError()
    }

    if (state.showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = onDismissDelete,
            title = { Text(stringResource(R.string.session_delete_title)) },
            text = { Text(stringResource(R.string.session_delete_body)) },
            confirmButton = {
                TextButton(onClick = onConfirmDelete) {
                    Text(stringResource(R.string.session_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDelete) {
                    Text(stringResource(R.string.session_delete_cancel))
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.session_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.detail != null) {
                        IconButton(onClick = onEdit) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = stringResource(R.string.session_edit),
                            )
                        }
                        IconButton(onClick = onRequestDelete) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.session_delete),
                            )
                        }
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
                    Text(
                        text = stringResource(R.string.session_loading),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            state.detail == null -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.session_not_found),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            else -> {
                SessionDetailContent(
                    detail = state.detail,
                    contentPadding = padding,
                )
            }
        }
    }
}

@Composable
private fun SessionDetailContent(
    detail: SessionDetail,
    contentPadding: PaddingValues,
) {
    val session = detail.session
    val jobName = detail.job?.name?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.session_fallback_name)
    val jobColor = detail.job?.colorArgb ?: 0xFF10B981.toInt()
    val endAt = session.endAt

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color(jobColor)),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(jobName, style = MaterialTheme.typography.titleLarge)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = statusLabel(session.status),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(12.dp))
                    MoneyText(
                        amountMinor = detail.breakdown.earnedMinor,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(
                            R.string.session_active_duration,
                            formatDuration(detail.breakdown.activeMillis),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            DetailSection(title = stringResource(R.string.session_times_title)) {
                DetailRow(
                    label = stringResource(R.string.session_start),
                    value = DateTimeFormat.formatLocalDateTime(session.startAt),
                )
                DetailRow(
                    label = stringResource(R.string.session_end),
                    value = endAt?.let { DateTimeFormat.formatLocalDateTime(it) }
                        ?: stringResource(R.string.session_end_open),
                )
                if (endAt != null) {
                    DetailRow(
                        label = stringResource(R.string.session_wall_duration),
                        value = formatDuration(endAt - session.startAt),
                    )
                }
            }
        }

        item {
            DetailSection(title = stringResource(R.string.session_earnings_title)) {
                DetailRow(
                    label = stringResource(R.string.session_regular),
                    value = stringResource(
                        R.string.session_minutes_value,
                        detail.breakdown.regularMinutes,
                    ),
                )
                DetailRow(
                    label = stringResource(R.string.session_ot),
                    value = stringResource(
                        R.string.session_minutes_value,
                        detail.breakdown.otMinutes,
                    ),
                )
                DetailRow(
                    label = stringResource(R.string.session_rate),
                    value = formatMoneyRate(session.snapshotHourlyRateMinor),
                )
                if (session.snapshotOtRateMinor != null) {
                    DetailRow(
                        label = stringResource(R.string.session_ot_rate),
                        value = formatMoneyRate(session.snapshotOtRateMinor),
                    )
                }
                DetailRow(
                    label = stringResource(R.string.session_earned),
                    value = formatMoney(detail.breakdown.earnedMinor),
                )
            }
        }

        item {
            DetailSection(title = stringResource(R.string.session_breaks_title)) {
                if (detail.breaks.isEmpty()) {
                    Text(
                        text = stringResource(R.string.session_breaks_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    detail.breaks.forEachIndexed { index, br ->
                        if (index > 0) {
                            HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        }
                        BreakRow(br)
                    }
                }
            }
        }

        item {
            DetailSection(title = stringResource(R.string.session_notes_title)) {
                Text(
                    text = session.notes?.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.session_notes_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (session.notes.isNullOrBlank()) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
            }
        }
    }
}

@Composable
private fun DetailSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun BreakRow(br: BreakInterval) {
    val endLabel = br.endAt?.let { DateTimeFormat.formatLocalDateTime(it) }
        ?: stringResource(R.string.session_break_open)
    val duration = br.endAt?.let { formatDuration(it - br.startAt) }
        ?: stringResource(R.string.session_break_open)
    Column {
        Text(
            text = stringResource(
                R.string.session_break_range,
                DateTimeFormat.formatLocalDateTime(br.startAt),
                endLabel,
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = duration,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun statusLabel(status: SessionStatus): String = when (status) {
    SessionStatus.RUNNING -> stringResource(R.string.session_status_running)
    SessionStatus.PAUSED -> stringResource(R.string.session_status_paused)
    SessionStatus.COMPLETED -> stringResource(R.string.session_status_completed)
    SessionStatus.CANCELLED -> stringResource(R.string.session_status_cancelled)
}

private fun formatDuration(millis: Long): String {
    val totalMinutes = maxOf(0L, millis) / 60_000L
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return if (hours > 0L) {
        String.format(Locale.US, "%d h %02d m", hours, minutes)
    } else {
        String.format(Locale.US, "%d m", minutes)
    }
}
