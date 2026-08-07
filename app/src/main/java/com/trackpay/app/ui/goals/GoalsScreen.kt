package com.trackpay.app.ui.goals

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trackpay.app.R
import com.trackpay.app.domain.model.GoalProgress
import com.trackpay.app.domain.model.GoalTemplate
import com.trackpay.app.ui.util.MoneyFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun GoalsRoute(
    onAddGoal: () -> Unit,
    onEditGoal: (String) -> Unit,
    onUseTemplate: (GoalTemplate) -> Unit,
    viewModel: GoalsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    GoalsScreen(
        state = state,
        onAddGoal = onAddGoal,
        onEditGoal = onEditGoal,
        onUseTemplate = onUseTemplate,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    state: GoalsUiState,
    onAddGoal: () -> Unit,
    onEditGoal: (String) -> Unit,
    onUseTemplate: (GoalTemplate) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.goals_title)) },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddGoal) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.goals_add),
                )
            }
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
                    text = stringResource(R.string.goals_loading),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "header") {
                GoalsHeaderCard(
                    totalSavedMinor = state.totalSavedMinor,
                    totalTargetMinor = state.totalTargetMinor,
                    overallProgress = state.overallProgress,
                    goalCount = state.goals.size,
                )
            }

            if (state.goals.isEmpty()) {
                item(key = "empty") {
                    Text(
                        text = stringResource(R.string.goals_empty_body),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            } else {
                item(key = "list_label") {
                    Text(
                        text = stringResource(R.string.goals_your_goals),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                items(state.goals, key = { it.goal.id }) { progress ->
                    GoalProgressRow(
                        progress = progress,
                        onClick = { onEditGoal(progress.goal.id) },
                    )
                }
            }

            item(key = "templates_label") {
                Text(
                    text = stringResource(R.string.goals_templates_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            item(key = "templates_grid") {
                TemplatesGrid(
                    templates = state.templates,
                    onUseTemplate = onUseTemplate,
                )
            }
            item(key = "fab_spacer") {
                Spacer(Modifier.height(72.dp))
            }
        }
    }
}

@Composable
private fun GoalsHeaderCard(
    totalSavedMinor: Long,
    totalTargetMinor: Long,
    overallProgress: Float,
    goalCount: Int,
) {
    val percentLabel = (overallProgress * 100f).roundToInt().coerceIn(0, 100)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.goals_header_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = MoneyFormat.format(totalSavedMinor),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = stringResource(
                    R.string.goals_header_of_target,
                    MoneyFormat.format(totalTargetMinor),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
            )
            LinearProgressIndicator(
                progress = { overallProgress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                strokeCap = StrokeCap.Round,
            )
            Text(
                text = stringResource(R.string.goals_header_percent, percentLabel, goalCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
            )
        }
    }
}

@Composable
private fun GoalProgressRow(
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
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = goalIcon(goal.iconKey),
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goal.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = formatDeadlineLabel(goal.deadlineEpochDay, progress.overdue),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (progress.overdue) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = color,
                )
            }
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(
                        R.string.goals_saved_of_target,
                        MoneyFormat.format(progress.savedMinor),
                        MoneyFormat.format(goal.targetMinor),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = if (progress.overdue) {
                        stringResource(R.string.goals_overdue)
                    } else {
                        stringResource(
                            R.string.goals_pace_per_week,
                            MoneyFormat.format(progress.pacePerWeekMinor),
                        )
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (progress.overdue) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            if (goal.allocationBps > 0) {
                Text(
                    text = stringResource(
                        R.string.goals_allocation_label,
                        formatBpsPercent(goal.allocationBps),
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TemplatesGrid(
    templates: List<GoalTemplate>,
    onUseTemplate: (GoalTemplate) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        templates.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                row.forEach { template ->
                    Box(modifier = Modifier.weight(1f)) {
                        TemplateCard(
                            template = template,
                            onClick = { onUseTemplate(template) },
                        )
                    }
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun TemplateCard(
    template: GoalTemplate,
    onClick: () -> Unit,
) {
    val color = Color(template.colorArgb)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = goalIcon(template.iconKey),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = template.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = MoneyFormat.format(template.defaultTargetMinor),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(
                    R.string.goals_template_horizon,
                    template.defaultHorizonMonths,
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun goalIcon(iconKey: String): ImageVector = when (iconKey) {
    "shield" -> Icons.Default.Shield
    "flight" -> Icons.Default.Flight
    "directions_car" -> Icons.Default.DirectionsCar
    "home" -> Icons.Default.Home
    "laptop" -> Icons.Default.Laptop
    "work" -> Icons.Default.Work
    "star" -> Icons.Default.Star
    else -> Icons.Default.Savings
}

private val DEADLINE_FMT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())

private fun formatDeadlineLabel(epochDay: Long, overdue: Boolean): String {
    val date = LocalDate.ofEpochDay(epochDay).format(DEADLINE_FMT)
    return if (overdue) "Due $date · Overdue" else "Due $date"
}

private fun formatBpsPercent(bps: Int): String {
    val percent = bps / 100f
    val rounded = (percent * 10f).roundToInt() / 10f
    return if (rounded % 1f == 0f) "${rounded.toInt()}%" else "$rounded%"
}
