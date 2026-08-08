package com.trackpay.app.ui.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trackpay.app.R
import com.trackpay.app.domain.model.SessionListItem
import com.trackpay.app.ui.components.MoneyText
import com.trackpay.app.ui.util.TimeFormat
import com.trackpay.app.ui.util.formatMoney

@Composable
fun HistoryRoute(
    onOpenSession: (String) -> Unit,
    onCreateSession: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryScreen(
        state = state,
        onOpenSession = onOpenSession,
        onCreateSession = onCreateSession,
        onQueryChange = viewModel::onQueryChange,
        onJobFilter = viewModel::onJobFilter,
        onRangePreset = viewModel::onRangePreset,
        onClearFilters = viewModel::clearFilters,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onOpenSession: (String) -> Unit,
    onCreateSession: () -> Unit,
    onQueryChange: (String) -> Unit,
    onJobFilter: (String?) -> Unit,
    onRangePreset: (HistoryRangePreset) -> Unit,
    onClearFilters: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.history_title)) })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateSession) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.history_create),
                )
            }
        },
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
                        text = stringResource(R.string.history_loading),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            !state.hasAnySessions -> {
                EmptyHistoryPane(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    onCreateSession = onCreateSession,
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp, top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(key = "summary") {
                        SummaryCard(state = state)
                    }
                    item(key = "filters") {
                        FiltersSection(
                            state = state,
                            onQueryChange = onQueryChange,
                            onJobFilter = onJobFilter,
                            onRangePreset = onRangePreset,
                            onClearFilters = onClearFilters,
                        )
                    }
                    if (state.groups.isEmpty()) {
                        item(key = "no-matches") {
                            NoMatchesPane()
                        }
                    } else {
                        state.groups.forEach { group ->
                            stickyHeader(key = "day-${group.dayStartMillis}") {
                                DayHeader(label = group.label)
                            }
                            items(group.items, key = { it.session.id }) { item ->
                                SessionRow(
                                    item = item,
                                    onClick = { onOpenSession(item.session.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(state: HistoryUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.history_summary_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            MoneyText(
                amountMinor = state.totals.earnedMinor,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(
                    R.string.history_summary_meta,
                    state.totals.shiftCount,
                    formatHours(state.totals.activeMillis),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.history_showing_shifts, state.totals.shiftCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FiltersSection(
    state: HistoryUiState,
    onQueryChange: (String) -> Unit,
    onJobFilter: (String?) -> Unit,
    onRangePreset: (HistoryRangePreset) -> Unit,
    onClearFilters: () -> Unit,
) {
    var jobMenuExpanded by remember { mutableStateOf(false) }
    val selectedJobName = state.jobs.firstOrNull { it.id == state.selectedJobId }?.name
        ?: stringResource(R.string.history_filter_all_jobs)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(stringResource(R.string.history_search_hint)) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null)
            },
            trailingIcon = {
                if (state.query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = stringResource(R.string.history_clear_search),
                        )
                    }
                }
            },
        )

        ExposedDropdownMenuBox(
            expanded = jobMenuExpanded,
            onExpandedChange = { jobMenuExpanded = it },
        ) {
            OutlinedTextField(
                value = selectedJobName,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                label = { Text(stringResource(R.string.history_filter_job)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = jobMenuExpanded) },
            )
            ExposedDropdownMenu(
                expanded = jobMenuExpanded,
                onDismissRequest = { jobMenuExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.history_filter_all_jobs)) },
                    onClick = {
                        onJobFilter(null)
                        jobMenuExpanded = false
                    },
                )
                state.jobs.forEach { job ->
                    DropdownMenuItem(
                        text = { Text(job.name) },
                        onClick = {
                            onJobFilter(job.id)
                            jobMenuExpanded = false
                        },
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RangeChip(
                label = stringResource(R.string.history_range_7d),
                selected = state.rangePreset == HistoryRangePreset.DAYS_7,
                onClick = { onRangePreset(HistoryRangePreset.DAYS_7) },
            )
            RangeChip(
                label = stringResource(R.string.history_range_30d),
                selected = state.rangePreset == HistoryRangePreset.DAYS_30,
                onClick = { onRangePreset(HistoryRangePreset.DAYS_30) },
            )
            RangeChip(
                label = stringResource(R.string.history_range_90d),
                selected = state.rangePreset == HistoryRangePreset.DAYS_90,
                onClick = { onRangePreset(HistoryRangePreset.DAYS_90) },
            )
            RangeChip(
                label = stringResource(R.string.history_range_all),
                selected = state.rangePreset == HistoryRangePreset.ALL,
                onClick = { onRangePreset(HistoryRangePreset.ALL) },
            )
            if (state.filtersActive) {
                TextButton(onClick = onClearFilters) {
                    Text(stringResource(R.string.history_clear_filters))
                }
            }
        }
    }
}

@Composable
private fun RangeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
    )
}

@Composable
private fun DayHeader(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = 8.dp),
    )
}

@Composable
private fun SessionRow(
    item: SessionListItem,
    onClick: () -> Unit,
) {
    val endAt = item.session.endAt
    val timeRange = if (endAt != null) {
        stringResource(
            R.string.history_row_time_range,
            TimeFormat.formatSince(item.session.startAt),
            TimeFormat.formatSince(endAt),
        )
    } else {
        TimeFormat.formatSince(item.session.startAt)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color(item.jobColorArgb)),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.jobName.ifBlank { stringResource(R.string.session_fallback_name) },
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = timeRange,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(
                        R.string.history_row_duration,
                        formatHours(item.activeMillis),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = formatMoney(item.earnedMinor),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun EmptyHistoryPane(
    onCreateSession: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.history_empty_title),
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.history_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onCreateSession) {
            Text(stringResource(R.string.history_create))
        }
    }
}

@Composable
private fun NoMatchesPane() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.history_no_matches_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.history_no_matches_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatHours(activeMillis: Long): String {
    val totalMinutes = maxOf(0L, activeMillis) / 60_000L
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return if (hours > 0L) {
        String.format(java.util.Locale.US, "%d h %02d m", hours, minutes)
    } else {
        String.format(java.util.Locale.US, "%d m", minutes)
    }
}
