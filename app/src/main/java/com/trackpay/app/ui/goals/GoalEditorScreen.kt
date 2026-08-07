package com.trackpay.app.ui.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
import com.trackpay.app.domain.model.GoalDefaults
import com.trackpay.app.domain.model.GoalTemplate
import kotlin.math.roundToInt

@Composable
fun GoalEditorRoute(
    goalId: String?,
    template: GoalTemplate? = null,
    onBack: () -> Unit,
    viewModel: GoalEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(goalId, template?.name) {
        if (template != null && (goalId == null || goalId == "new")) {
            viewModel.applyTemplate(template)
        } else {
            viewModel.load(goalId)
        }
    }
    LaunchedEffect(state.saved, state.archived) {
        if (state.saved || state.archived) onBack()
    }
    GoalEditorScreen(
        state = state,
        onBack = onBack,
        onNameChange = viewModel::onNameChange,
        onTargetChange = viewModel::onTargetChange,
        onDeadlineChange = viewModel::onDeadlineChange,
        onColorChange = viewModel::onColorChange,
        onIconChange = viewModel::onIconChange,
        onAllocationPercentTextChange = viewModel::onAllocationPercentTextChange,
        onAllocationSliderChange = viewModel::onAllocationSliderChange,
        onSave = viewModel::save,
        onArchive = viewModel::archive,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalEditorScreen(
    state: GoalEditorUiState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onTargetChange: (String) -> Unit,
    onDeadlineChange: (String) -> Unit,
    onColorChange: (Int) -> Unit,
    onIconChange: (String) -> Unit,
    onAllocationPercentTextChange: (String) -> Unit,
    onAllocationSliderChange: (Float) -> Unit,
    onSave: () -> Unit,
    onArchive: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.isNew) {
                            stringResource(R.string.goals_add)
                        } else {
                            stringResource(R.string.goals_edit)
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
                        Text(stringResource(R.string.goals_save))
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
                label = { Text(stringResource(R.string.goals_name)) },
                singleLine = true,
            )
            OutlinedTextField(
                value = state.targetText,
                onValueChange = onTargetChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.goals_target)) },
                singleLine = true,
                prefix = { Text("$") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
            OutlinedTextField(
                value = state.deadlineText,
                onValueChange = onDeadlineChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.goals_deadline)) },
                supportingText = { Text(stringResource(R.string.goals_deadline_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
            )

            Text(
                text = stringResource(R.string.goals_color),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items(GoalDefaults.COLOR_PRESETS.size) { index ->
                    val colorArgb = GoalDefaults.COLOR_PRESETS[index]
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
                text = stringResource(R.string.goals_icon),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(GoalIconPresets.ALL, key = { it }) { iconKey ->
                    val label = iconKey
                        .replace('_', ' ')
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                    FilterChip(
                        selected = iconKey == state.iconKey,
                        onClick = { onIconChange(iconKey) },
                        label = { Text(label) },
                        leadingIcon = {
                            Icon(
                                imageVector = goalIcon(iconKey),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                    )
                }
            }

            Text(
                text = stringResource(R.string.goals_allocation),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = state.allocationPercentText,
                onValueChange = onAllocationPercentTextChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.goals_allocation_percent)) },
                singleLine = true,
                suffix = { Text("%") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
            Slider(
                value = state.allocationPercent,
                onValueChange = onAllocationSliderChange,
                valueRange = 0f..100f,
                steps = 19,
            )
            val totalPercent = state.projectedTotalBps / 100f
            val totalLabel = if (totalPercent % 1f == 0f) {
                totalPercent.toInt().toString()
            } else {
                ((totalPercent * 10f).roundToInt() / 10f).toString()
            }
            Text(
                text = stringResource(R.string.goals_allocation_total, totalLabel),
                style = MaterialTheme.typography.bodySmall,
                color = if (state.allocationOverLimit) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            if (state.allocationOverLimit) {
                Text(
                    text = stringResource(R.string.goals_allocation_over_limit),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
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

            if (state.canArchive) {
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onArchive) {
                    Text(
                        text = stringResource(R.string.goals_archive),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}
