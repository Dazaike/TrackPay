package com.trackpay.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackpay.app.domain.model.GoalDefaults
import com.trackpay.app.domain.model.GoalTemplate
import com.trackpay.app.domain.model.JobDefaults
import com.trackpay.app.domain.usecase.ListGoalTemplatesUseCase
import com.trackpay.app.domain.usecase.MaterializeGoalTemplateUseCase
import com.trackpay.app.domain.usecase.SetOnboardingDoneUseCase
import com.trackpay.app.domain.usecase.UpsertJobUseCase
import com.trackpay.app.ui.util.MoneyFormat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class OnboardingStep {
    Welcome,
    CreateJob,
    OptionalGoal,
    Permissions,
}

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.Welcome,
    val jobName: String = "",
    val hourlyRateText: String = "",
    val templates: List<GoalTemplate> = emptyList(),
    val selectedTemplate: GoalTemplate? = null,
    val errorMessage: String? = null,
    val finished: Boolean = false,
    val saving: Boolean = false,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    listGoalTemplates: ListGoalTemplatesUseCase,
    private val upsertJob: UpsertJobUseCase,
    private val materializeGoalTemplate: MaterializeGoalTemplateUseCase,
    private val setOnboardingDone: SetOnboardingDoneUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        OnboardingUiState(templates = listGoalTemplates()),
    )
    val uiState: StateFlow<OnboardingUiState> = _uiState

    fun onJobNameChange(value: String) {
        _uiState.update { it.copy(jobName = value, errorMessage = null) }
    }

    fun onHourlyRateChange(value: String) {
        _uiState.update { it.copy(hourlyRateText = value, errorMessage = null) }
    }

    fun onSelectTemplate(template: GoalTemplate?) {
        _uiState.update { it.copy(selectedTemplate = template) }
    }

    fun nextFromWelcome() {
        _uiState.update { it.copy(step = OnboardingStep.CreateJob, errorMessage = null) }
    }

    fun nextFromJob() {
        val s = _uiState.value
        val name = s.jobName.trim()
        if (name.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Give your job a name") }
            return
        }
        val hourly = MoneyFormat.parseMajorToMinor(s.hourlyRateText)
        if (hourly == null || hourly <= 0L) {
            _uiState.update { it.copy(errorMessage = "Enter a valid hourly rate") }
            return
        }
        _uiState.update {
            it.copy(step = OnboardingStep.OptionalGoal, errorMessage = null)
        }
    }

    fun nextFromGoal() {
        _uiState.update {
            it.copy(step = OnboardingStep.Permissions, errorMessage = null)
        }
    }

    fun skipGoal() {
        _uiState.update {
            it.copy(
                selectedTemplate = null,
                step = OnboardingStep.Permissions,
                errorMessage = null,
            )
        }
    }

    fun back() {
        _uiState.update { state ->
            val previous = when (state.step) {
                OnboardingStep.Welcome -> OnboardingStep.Welcome
                OnboardingStep.CreateJob -> OnboardingStep.Welcome
                OnboardingStep.OptionalGoal -> OnboardingStep.CreateJob
                OnboardingStep.Permissions -> OnboardingStep.OptionalGoal
            }
            state.copy(step = previous, errorMessage = null)
        }
    }

    fun finish() {
        val s = _uiState.value
        if (s.saving || s.finished) return
        val name = s.jobName.trim()
        val hourly = MoneyFormat.parseMajorToMinor(s.hourlyRateText)
        if (name.isEmpty() || hourly == null || hourly <= 0L) {
            _uiState.update {
                it.copy(
                    step = OnboardingStep.CreateJob,
                    errorMessage = "Job name and hourly rate are required",
                )
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(saving = true, errorMessage = null) }
            runCatching {
                upsertJob(
                    name = name,
                    hourlyRateMinor = hourly,
                    colorArgb = JobDefaults.DEFAULT_COLOR_ARGB,
                    iconKey = JobDefaults.DEFAULT_ICON_KEY,
                )
                s.selectedTemplate?.let { template ->
                    materializeGoalTemplate(
                        template = template,
                        allocationBps = GoalDefaults.DEFAULT_ALLOCATION_BPS,
                    )
                }
                setOnboardingDone(true)
            }.onSuccess {
                _uiState.update { it.copy(saving = false, finished = true) }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        saving = false,
                        errorMessage = err.message ?: "Couldn’t finish setup",
                    )
                }
            }
        }
    }
}
