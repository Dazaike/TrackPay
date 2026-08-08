package com.trackpay.app.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackpay.app.domain.model.AchievementStatus
import com.trackpay.app.domain.model.InsightsMetric
import com.trackpay.app.domain.model.InsightsRange
import com.trackpay.app.domain.model.InsightsRangeSummary
import com.trackpay.app.domain.model.StreakState
import com.trackpay.app.domain.model.WeekdayAverage
import com.trackpay.app.domain.model.WeeklyChallenge
import com.trackpay.app.domain.usecase.EvaluateAchievementsUseCase
import com.trackpay.app.domain.usecase.ObserveInsightsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InsightsUiState(
    val loading: Boolean = true,
    val hasData: Boolean = false,
    val challenge: WeeklyChallenge? = null,
    val range: InsightsRange = InsightsRange.D7,
    val metric: InsightsMetric = InsightsMetric.EARNINGS,
    val rangeSummary: InsightsRangeSummary? = null,
    val weekdays: List<WeekdayAverage> = emptyList(),
    val streak: StreakState = StreakState(
        currentDays = 0,
        bestDays = 0,
        lastActiveLocalDateEpochDay = null,
    ),
    val achievements: List<AchievementStatus> = emptyList(),
) {
    val maxWeekdayIndex: Int
        get() {
            if (weekdays.isEmpty()) return -1
            val values = weekdays.map { weekdayValue(it, metric) }
            val max = values.maxOrNull() ?: return -1
            if (max <= 0L) return -1
            return values.indexOfFirst { it == max }
        }
}

private fun weekdayValue(day: WeekdayAverage, metric: InsightsMetric): Long =
    when (metric) {
        InsightsMetric.EARNINGS -> day.earnedMinor
        InsightsMetric.HOURS -> day.activeMillis
    }

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class InsightsViewModel @Inject constructor(
    observeInsights: ObserveInsightsUseCase,
    evaluateAchievements: EvaluateAchievementsUseCase,
) : ViewModel() {

    private val range = MutableStateFlow(InsightsRange.D7)
    private val metric = MutableStateFlow(InsightsMetric.EARNINGS)

    init {
        viewModelScope.launch {
            runCatching { evaluateAchievements() }
        }
    }

    val uiState: StateFlow<InsightsUiState> = combine(
        range.flatMapLatest { selected -> observeInsights(selected) },
        range,
        metric,
    ) { snapshot, selectedRange, selectedMetric ->
        InsightsUiState(
            loading = false,
            hasData = snapshot.hasCompletedSessions,
            challenge = snapshot.weeklyChallenge,
            range = selectedRange,
            metric = selectedMetric,
            rangeSummary = snapshot.rangeSummary,
            weekdays = snapshot.weekdayAverages,
            streak = snapshot.streak,
            achievements = snapshot.achievements,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = InsightsUiState(),
    )

    fun selectRange(value: InsightsRange) {
        range.value = value
    }

    fun selectMetric(value: InsightsMetric) {
        metric.value = value
    }
}
