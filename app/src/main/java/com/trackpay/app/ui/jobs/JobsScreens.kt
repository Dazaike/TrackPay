package com.trackpay.app.ui.jobs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trackpay.app.R
import com.trackpay.app.domain.model.Job
import com.trackpay.app.domain.model.JobDefaults
import com.trackpay.app.ui.util.formatMoney
import com.trackpay.app.ui.util.formatMoneyRate
import com.trackpay.app.ui.util.currencySymbol

@Composable
fun JobsListRoute(
    onBack: () -> Unit,
    onAddJob: () -> Unit,
    onEditJob: (String) -> Unit,
    viewModel: JobsListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    JobsListScreen(
        state = state,
        onBack = onBack,
        onAddJob = onAddJob,
        onEditJob = onEditJob,
        onArchive = viewModel::archive,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobsListScreen(
    state: JobsListUiState,
    onBack: () -> Unit,
    onAddJob: () -> Unit,
    onEditJob: (String) -> Unit,
    onArchive: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.jobs_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddJob) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.jobs_add))
            }
        },
    ) { padding ->
        if (state.jobs.isEmpty()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.jobs_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.jobs, key = { it.id }) { job ->
                    JobRow(
                        job = job,
                        onClick = { onEditJob(job.id) },
                        onArchive = { onArchive(job.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun JobRow(
    job: Job,
    onClick: () -> Unit,
    onArchive: () -> Unit,
) {
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
                    .background(Color(job.colorArgb)),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(job.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    formatMoneyRate(job.hourlyRateMinor),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (job.otRateMinor != null) {
                    Text(
                        "OT ${formatMoneyRate(job.otRateMinor)} after ${job.otThresholdMinutes ?: 480}m",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = onArchive) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.jobs_archive),
                )
            }
        }
    }
}

@Composable
fun JobEditorRoute(
    jobId: String?,
    onBack: () -> Unit,
    viewModel: JobEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(jobId) { viewModel.load(jobId) }
    LaunchedEffect(state.saved) {
        if (state.saved) onBack()
    }
    JobEditorScreen(
        state = state,
        onBack = onBack,
        onNameChange = viewModel::onNameChange,
        onHourlyRateChange = viewModel::onHourlyRateChange,
        onOtRateChange = viewModel::onOtRateChange,
        onOtThresholdChange = viewModel::onOtThresholdChange,
        onColorChange = viewModel::onColorChange,
        onIconChange = viewModel::onIconChange,
        onGeoEnabledChange = viewModel::onGeoEnabledChange,
        onLatitudeChange = viewModel::onLatitudeChange,
        onLongitudeChange = viewModel::onLongitudeChange,
        onRadiusChange = viewModel::onRadiusChange,
        onSave = viewModel::save,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobEditorScreen(
    state: JobEditorUiState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onHourlyRateChange: (String) -> Unit,
    onOtRateChange: (String) -> Unit,
    onOtThresholdChange: (String) -> Unit,
    onColorChange: (Int) -> Unit,
    onIconChange: (String) -> Unit,
    onGeoEnabledChange: (Boolean) -> Unit,
    onLatitudeChange: (String) -> Unit,
    onLongitudeChange: (String) -> Unit,
    onRadiusChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.isNew) {
                            stringResource(R.string.jobs_add)
                        } else {
                            stringResource(R.string.jobs_edit)
                        },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = onSave) {
                        Text(stringResource(R.string.jobs_save))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.jobs_name)) },
                singleLine = true,
            )
            OutlinedTextField(
                value = state.hourlyRateText,
                onValueChange = onHourlyRateChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.jobs_hourly_rate)) },
                singleLine = true,
                prefix = { Text(currencySymbol()) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
            OutlinedTextField(
                value = state.otRateText,
                onValueChange = onOtRateChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.jobs_ot_rate)) },
                singleLine = true,
                prefix = { Text(currencySymbol()) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
            OutlinedTextField(
                value = state.otThresholdHoursText,
                onValueChange = onOtThresholdChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.jobs_ot_threshold)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                enabled = state.otRateText.isNotBlank(),
            )
            Text(
                text = stringResource(R.string.jobs_color),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items(JobDefaults.COLOR_PRESETS.size) { index ->
                    val colorArgb = JobDefaults.COLOR_PRESETS[index]
                    val selected = colorArgb == state.colorArgb
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(colorArgb))
                            .then(
                                if (selected) {
                                    Modifier.border(
                                        width = 3.dp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        shape = CircleShape,
                                    )
                                } else {
                                    Modifier
                                },
                            )
                            .clickable { onColorChange(colorArgb) },
                    )
                }
            }
            Text(
                text = stringResource(R.string.jobs_icon),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(JobDefaults.ICON_PRESETS, key = { it }) { iconKey ->
                    val label = iconKey
                        .replace('_', ' ')
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                    FilterChip(
                        selected = iconKey == state.iconKey,
                        onClick = { onIconChange(iconKey) },
                        label = { Text(label) },
                    )
                }
            }
            Text(
                text = stringResource(R.string.jobs_location_section),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.jobs_location_help),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.jobs_geo_enabled),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Switch(
                    checked = state.geoEnabled,
                    onCheckedChange = onGeoEnabledChange,
                )
            }
            if (state.geoEnabled) {
                OutlinedTextField(
                    value = state.latitudeText,
                    onValueChange = onLatitudeChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.jobs_latitude)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                OutlinedTextField(
                    value = state.longitudeText,
                    onValueChange = onLongitudeChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.jobs_longitude)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                OutlinedTextField(
                    value = state.radiusText,
                    onValueChange = onRadiusChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.jobs_radius_meters)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
            if (state.errorMessage != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = state.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
