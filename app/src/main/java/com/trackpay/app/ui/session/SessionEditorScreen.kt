package com.trackpay.app.ui.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trackpay.app.R

@Composable
fun SessionEditorRoute(
    sessionId: String?,
    onBack: () -> Unit,
    viewModel: SessionEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(sessionId) { viewModel.load(sessionId) }
    LaunchedEffect(state.saved) {
        if (state.saved) onBack()
    }
    SessionEditorScreen(
        state = state,
        onBack = onBack,
        onJobSelected = viewModel::onJobSelected,
        onStartChange = viewModel::onStartChange,
        onEndChange = viewModel::onEndChange,
        onNotesChange = viewModel::onNotesChange,
        onApplyCurrentRatesChange = viewModel::onApplyCurrentRatesChange,
        onAddBreak = viewModel::addBreak,
        onRemoveBreak = viewModel::removeBreak,
        onBreakStartChange = viewModel::onBreakStartChange,
        onBreakEndChange = viewModel::onBreakEndChange,
        onSave = viewModel::save,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionEditorScreen(
    state: SessionEditorUiState,
    onBack: () -> Unit,
    onJobSelected: (String) -> Unit,
    onStartChange: (String) -> Unit,
    onEndChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onApplyCurrentRatesChange: (Boolean) -> Unit,
    onAddBreak: () -> Unit,
    onRemoveBreak: (String) -> Unit,
    onBreakStartChange: (String, String) -> Unit,
    onBreakEndChange: (String, String) -> Unit,
    onSave: () -> Unit,
) {
    var jobMenuExpanded by remember { mutableStateOf(false) }
    val selectedJobName = state.jobs.firstOrNull { it.id == state.selectedJobId }?.name
        ?: stringResource(R.string.session_select_job)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.isNew) {
                            stringResource(R.string.session_create_title)
                        } else {
                            stringResource(R.string.session_edit_title)
                        },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = onSave, enabled = !state.loading) {
                        Text(stringResource(R.string.session_save))
                    }
                },
            )
        },
    ) { padding ->
        if (state.loading) {
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
            return@Scaffold
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
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
                    label = { Text(stringResource(R.string.session_job)) },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = jobMenuExpanded)
                    },
                )
                ExposedDropdownMenu(
                    expanded = jobMenuExpanded,
                    onDismissRequest = { jobMenuExpanded = false },
                ) {
                    if (state.jobs.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.session_no_jobs)) },
                            onClick = { jobMenuExpanded = false },
                            enabled = false,
                        )
                    } else {
                        state.jobs.forEach { job ->
                            DropdownMenuItem(
                                text = { Text(job.name) },
                                onClick = {
                                    onJobSelected(job.id)
                                    jobMenuExpanded = false
                                },
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = state.startText,
                onValueChange = onStartChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.session_start)) },
                supportingText = { Text(stringResource(R.string.session_datetime_hint)) },
            )
            OutlinedTextField(
                value = state.endText,
                onValueChange = onEndChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.session_end)) },
                supportingText = { Text(stringResource(R.string.session_datetime_hint)) },
            )
            OutlinedTextField(
                value = state.notes,
                onValueChange = onNotesChange,
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                label = { Text(stringResource(R.string.session_notes)) },
            )

            if (!state.isNew) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.session_apply_current_rates),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = stringResource(R.string.session_apply_current_rates_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = state.applyCurrentJobRates,
                            onCheckedChange = onApplyCurrentRatesChange,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.session_breaks_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onAddBreak) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text(stringResource(R.string.session_add_break))
                }
            }

            if (state.breaks.isEmpty()) {
                Text(
                    text = stringResource(R.string.session_breaks_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                state.breaks.forEach { draft ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        ),
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = stringResource(R.string.session_break_item),
                                    style = MaterialTheme.typography.labelLarge,
                                )
                                IconButton(onClick = { onRemoveBreak(draft.id) }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.session_remove_break),
                                    )
                                }
                            }
                            OutlinedTextField(
                                value = draft.startText,
                                onValueChange = { onBreakStartChange(draft.id, it) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                label = { Text(stringResource(R.string.session_break_start)) },
                                supportingText = {
                                    Text(stringResource(R.string.session_datetime_hint))
                                },
                            )
                            OutlinedTextField(
                                value = draft.endText,
                                onValueChange = { onBreakEndChange(draft.id, it) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                label = { Text(stringResource(R.string.session_break_end)) },
                                supportingText = {
                                    Text(stringResource(R.string.session_datetime_hint))
                                },
                            )
                        }
                    }
                }
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
