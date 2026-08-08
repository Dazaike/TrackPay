package com.trackpay.app.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackpay.app.domain.model.GoalProgress
import com.trackpay.app.domain.model.GoalTemplate
import com.trackpay.app.domain.usecase.ListGoalTemplatesUseCase
import com.trackpay.app.domain.usecase.ObserveGoalProgressUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class GoalsUiState(
    val loading: Boolean = true,
    val goals: List<GoalProgress> = emptyList(),
    val templates: List<GoalTemplate> = emptyList(),
    val totalSavedMinor: Long = 0L,
    val totalTargetMinor: Long = 0L,
    val overallProgress: Float = 0f,
) {
    val hasGoals: Boolean get() = goals.isNotEmpty()
}

@HiltViewModel
class GoalsViewModel @Inject constructor(
    observeGoalProgress: ObserveGoalProgressUseCase,
    listGoalTemplates: ListGoalTemplatesUseCase,
) : ViewModel() {

    val uiState: StateFlow<GoalsUiState> = combine(
        observeGoalProgress(),
        flowOf(listGoalTemplates()),
    ) { goals, templates ->
        val totalSaved = goals.sumOf { it.savedMinor }
        val totalTarget = goals.sumOf { it.goal.targetMinor.coerceAtLeast(0L) }
        val overall = if (totalTarget <= 0L) {
            0f
        } else {
            (totalSaved.toDouble() / totalTarget.toDouble()).toFloat().coerceIn(0f, 1f)
        }
        GoalsUiState(
            loading = false,
            goals = goals,
            templates = templates,
            totalSavedMinor = totalSaved,
            totalTargetMinor = totalTarget,
            overallProgress = overall,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = GoalsUiState(),
    )
}
