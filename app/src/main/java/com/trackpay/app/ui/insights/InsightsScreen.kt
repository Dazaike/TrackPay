package com.trackpay.app.ui.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trackpay.app.R
import com.trackpay.app.domain.model.AchievementStatus
import com.trackpay.app.domain.model.InsightsMetric
import com.trackpay.app.domain.model.InsightsRange
import com.trackpay.app.domain.model.InsightsRangeSummary
import com.trackpay.app.domain.model.RangeBucket
import com.trackpay.app.domain.model.StreakState
import com.trackpay.app.domain.model.WeekdayAverage
import com.trackpay.app.domain.model.WeeklyChallenge
import com.trackpay.app.ui.components.MoneyText
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt
import com.trackpay.app.ui.util.formatMoney

@Composable
fun InsightsRoute(
    viewModel: InsightsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    InsightsScreen(
        state = state,
        onSelectRange = viewModel::selectRange,
        onSelectMetric = viewModel::selectMetric,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    state: InsightsUiState,
    onSelectRange: (InsightsRange) -> Unit,
    onSelectMetric: (InsightsMetric) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.insights_title)) })
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
                        text = stringResource(R.string.insights_loading),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            !state.hasData -> {
                EmptyInsightsPane(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    state.challenge?.let { challenge ->
                        item(key = "challenge") {
                            WeeklyChallengeCard(challenge = challenge)
                        }
                    }

                    item(key = "range_controls") {
                        RangeControls(
                            range = state.range,
                            metric = state.metric,
                            onSelectRange = onSelectRange,
                            onSelectMetric = onSelectMetric,
                        )
                    }

                    state.rangeSummary?.let { summary ->
                        item(key = "chart") {
                            RangeChartCard(
                                summary = summary,
                                metric = state.metric,
                            )
                        }
                    }

                    if (state.weekdays.isNotEmpty()) {
                        item(key = "weekdays") {
                            WeekdayAveragesCard(
                                weekdays = state.weekdays,
                                metric = state.metric,
                                maxIndex = state.maxWeekdayIndex,
                            )
                        }
                    }

                    item(key = "streak") {
                        StreakCard(streak = state.streak)
                    }

                    if (state.achievements.isNotEmpty()) {
                        item(key = "achievements_label") {
                            Text(
                                text = stringResource(R.string.insights_achievements_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        item(key = "achievements") {
                            AchievementsStrip(items = state.achievements)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyInsightsPane(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.insights_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.insights_empty_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun WeeklyChallengeCard(challenge: WeeklyChallenge) {
    val progress = if (challenge.targetMinor <= 0L) {
        0f
    } else {
        (challenge.earnedMinor.toDouble() / challenge.targetMinor.toDouble())
            .toFloat()
            .coerceIn(0f, 1f)
    }
    val percent = (progress * 100f).roundToInt().coerceIn(0, 100)

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
                text = stringResource(R.string.insights_challenge_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = stringResource(
                    R.string.insights_challenge_progress,
                    formatMoney(challenge.earnedMinor),
                    formatMoney(challenge.targetMinor),
                ),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                strokeCap = StrokeCap.Round,
            )
            Text(
                text = stringResource(
                    R.string.insights_challenge_meta,
                    percent,
                    challenge.daysLeft,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
            )
        }
    }
}

@Composable
private fun RangeControls(
    range: InsightsRange,
    metric: InsightsMetric,
    onSelectRange: (InsightsRange) -> Unit,
    onSelectMetric: (InsightsMetric) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            InsightsRange.entries.forEach { value ->
                FilterChip(
                    selected = range == value,
                    onClick = { onSelectRange(value) },
                    label = { Text(rangeLabel(value)) },
                )
            }
        }

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = metric == InsightsMetric.EARNINGS,
                onClick = { onSelectMetric(InsightsMetric.EARNINGS) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            ) {
                Text(stringResource(R.string.insights_metric_earnings))
            }
            SegmentedButton(
                selected = metric == InsightsMetric.HOURS,
                onClick = { onSelectMetric(InsightsMetric.HOURS) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            ) {
                Text(stringResource(R.string.insights_metric_hours))
            }
        }
    }
}

@Composable
private fun RangeChartCard(
    summary: InsightsRangeSummary,
    metric: InsightsMetric,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.insights_range_total),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            when (metric) {
                InsightsMetric.EARNINGS -> {
                    MoneyText(
                        amountMinor = summary.earnedMinor,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    )
                }
                InsightsMetric.HOURS -> {
                    Text(
                        text = formatHoursLabel(summary.activeMillis),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            if (summary.buckets.isEmpty()) {
                Text(
                    text = stringResource(R.string.insights_chart_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                SimpleBarChart(
                    buckets = summary.buckets,
                    metric = metric,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                )
            }
        }
    }
}

@Composable
private fun SimpleBarChart(
    buckets: List<RangeBucket>,
    metric: InsightsMetric,
    modifier: Modifier = Modifier,
) {
    val values = buckets.map { bucketValue(it, metric) }
    val maxValue = max(1L, values.maxOrNull() ?: 1L)
    val barColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        buckets.forEachIndexed { index, bucket ->
            val fraction = (values[index].toDouble() / maxValue.toDouble()).toFloat().coerceIn(0f, 1f)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.72f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(trackColor.copy(alpha = 0.35f)),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.72f)
                            .fillMaxHeight(fraction.coerceAtLeast(if (values[index] > 0L) 0.04f else 0f))
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(barColor),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = bucket.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun WeekdayAveragesCard(
    weekdays: List<WeekdayAverage>,
    metric: InsightsMetric,
    maxIndex: Int,
) {
    val values = weekdays.map { weekdayValue(it, metric) }
    val maxValue = max(1L, values.maxOrNull() ?: 1L)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.insights_weekday_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                weekdays.forEachIndexed { index, day ->
                    val isMax = index == maxIndex
                    val fraction = (values[index].toDouble() / maxValue.toDouble())
                        .toFloat()
                        .coerceIn(0f, 1f)
                    val color = if (isMax) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .fillMaxHeight(fraction.coerceAtLeast(if (values[index] > 0L) 0.05f else 0f))
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(color),
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = weekdayShortLabel(day.dayOfWeek),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isMax) FontWeight.Bold else FontWeight.Normal,
                            color = if (isMax) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StreakCard(streak: StreakState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.insights_streak_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(
                        R.string.insights_streak_current,
                        streak.currentDays,
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(
                        R.string.insights_streak_best,
                        streak.bestDays,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AchievementsStrip(items: List<AchievementStatus>) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 2.dp),
    ) {
        items(items, key = { it.def.id }) { item ->
            AchievementCard(item = item)
        }
    }
}

@Composable
private fun AchievementCard(item: AchievementStatus) {
    val unlocked = item.unlocked
    val container = if (unlocked) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    }
    val onContainer = if (unlocked) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier.width(148.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = if (unlocked) Icons.Default.EmojiEvents else Icons.Outlined.EmojiEvents,
                contentDescription = null,
                tint = onContainer,
            )
            Text(
                text = item.def.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = onContainer,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.def.description,
                style = MaterialTheme.typography.bodySmall,
                color = onContainer.copy(alpha = 0.85f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(
                    if (unlocked) R.string.insights_achievement_unlocked
                    else R.string.insights_achievement_locked,
                ),
                style = MaterialTheme.typography.labelMedium,
                color = onContainer,
            )
        }
    }
}

@Composable
private fun rangeLabel(range: InsightsRange): String =
    when (range) {
        InsightsRange.D7 -> stringResource(R.string.insights_range_7d)
        InsightsRange.D30 -> stringResource(R.string.insights_range_30d)
        InsightsRange.D90 -> stringResource(R.string.insights_range_90d)
        InsightsRange.Y1 -> stringResource(R.string.insights_range_1y)
    }

private fun bucketValue(bucket: RangeBucket, metric: InsightsMetric): Long =
    when (metric) {
        InsightsMetric.EARNINGS -> bucket.earnedMinor
        InsightsMetric.HOURS -> bucket.activeMillis
    }

private fun weekdayValue(day: WeekdayAverage, metric: InsightsMetric): Long =
    when (metric) {
        InsightsMetric.EARNINGS -> day.earnedMinor
        InsightsMetric.HOURS -> day.activeMillis
    }

private fun weekdayShortLabel(isoDay: Int): String {
    val day = DayOfWeek.of(isoDay.coerceIn(1, 7))
    return day.getDisplayName(TextStyle.SHORT, Locale.getDefault())
}

private fun formatHoursLabel(activeMillis: Long): String {
    val totalMinutes = maxOf(0L, activeMillis) / 60_000L
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return if (hours > 0L) {
        String.format(Locale.getDefault(), "%d h %02d m", hours, minutes)
    } else {
        String.format(Locale.getDefault(), "%d m", minutes)
    }
}
