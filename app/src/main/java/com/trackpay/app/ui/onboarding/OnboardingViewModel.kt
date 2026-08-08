package com.trackpay.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackpay.app.domain.calc.GoalMath
import com.trackpay.app.domain.model.GoalDefaults
import com.trackpay.app.domain.model.GoalTemplate
import com.trackpay.app.domain.model.JobDefaults
import com.trackpay.app.domain.time.Clock
import com.trackpay.app.domain.usecase.ListGoalTemplatesUseCase
import com.trackpay.app.domain.usecase.MaterializeGoalTemplateUseCase
import com.trackpay.app.domain.usecase.ObserveCurrencyCodeUseCase
import com.trackpay.app.domain.usecase.SetCurrencyCodeUseCase
import com.trackpay.app.domain.usecase.SetOnboardingDoneUseCase
import com.trackpay.app.domain.usecase.UpsertGoalUseCase
import com.trackpay.app.domain.usecase.UpsertJobUseCase
import com.trackpay.app.ui.util.CurrencyFormat
import com.trackpay.app.ui.util.MoneyFormat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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
    val currencyCode: String = JobDefaults.DEFAULT_CURRENCY_CODE,
    val currencyCodes: List<String> = CurrencyFormat.COMMON_CODES,
    val templates: List<GoalTemplate> = emptyList(),
    val selectedTemplate: GoalTemplate? = null,
    val customGoalSelected: Boolean = false,
    val customGoalName: String = "",
    val customGoalTargetText: String = "",
    val errorMessage: String? = null,
    val finished: Boolean = false,
    val saving: Boolean = false,
) {
    val hasGoalSelection: Boolean
        get() = selectedTemplate != null || customGoalSelected
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    listGoalTemplates: ListGoalTemplatesUseCase,
    observeCurrencyCode: ObserveCurrencyCodeUseCase,
    private val setCurrencyCode: SetCurrencyCodeUseCase,
    private val upsertJob: UpsertJobUseCase,
    private val upsertGoal: UpsertGoalUseCase,
    private val materializeGoalTemplate: MaterializeGoalTemplateUseCase,
    private val setOnboardingDone: SetOnboardingDoneUseCase,
    private val clock: Clock,
) : ViewModel() {

    private val form = MutableStateFlow(
        OnboardingUiState(templates = listGoalTemplates()),
    )

    val uiState: StateFlow<OnboardingUiState> = combine(
        form,
        observeCurrencyCode(),
    ) { state, currency ->
        state.copy(currencyCode = currency)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = form.value,
    )

    fun onJobNameChange(value: String) {
        form.update { it.copy(jobName = value, errorMessage = null) }
    }

    fun onHourlyRateChange(value: String) {
        form.update { it.copy(hourlyRateText = value, errorMessage = null) }
    }

    fun onCurrencySelected(code: String) {
        viewModelScope.launch {
            setCurrencyCode(code)
            form.update { it.copy(errorMessage = null) }
        }
    }

    fun onSelectTemplate(template: GoalTemplate?) {
        form.update {
            it.copy(
                selectedTemplate = template,
                customGoalSelected = false,
                errorMessage = null,
            )
        }
    }

    fun onSelectCustomGoal() {
        form.update {
            it.copy(
                selectedTemplate = null,
                customGoalSelected = !it.customGoalSelected,
                errorMessage = null,
            )
        }
    }

    fun onCustomGoalNameChange(value: String) {
        form.update {
            it.copy(
                customGoalName = value,
                customGoalSelected = true,
                selectedTemplate = null,
                errorMessage = null,
            )
        }
    }

    fun onCustomGoalTargetChange(value: String) {
        form.update {
            it.copy(
                customGoalTargetText = value,
                customGoalSelected = true,
                selectedTemplate = null,
                errorMessage = null,
            )
        }
    }

    fun nextFromWelcome() {
        form.update { it.copy(step = OnboardingStep.CreateJob, errorMessage = null) }
    }

    fun nextFromJob() {
        val s = form.value
        val name = s.jobName.trim()
        if (name.isEmpty()) {
            form.update { it.copy(errorMessage = "Give your job a name") }
            return
        }
        val hourly = MoneyFormat.parseMajorToMinor(s.hourlyRateText, s.currencyCode)
        if (hourly == null || hourly <= 0L) {
            form.update { it.copy(errorMessage = "Enter a valid hourly rate") }
            return
        }
        form.update {
            it.copy(step = OnboardingStep.OptionalGoal, errorMessage = null)
        }
    }

    fun nextFromGoal() {
        val s = form.value
        if (s.customGoalSelected) {
            val name = s.customGoalName.trim()
            if (name.isEmpty()) {
                form.update { it.copy(errorMessage = "Name your goal") }
                return
            }
            val target = MoneyFormat.parseMajorToMinor(s.customGoalTargetText, s.currencyCode)
            if (target == null || target <= 0L) {
                form.update { it.copy(errorMessage = "Enter a valid target amount") }
                return
            }
        }
        form.update {
            it.copy(step = OnboardingStep.Permissions, errorMessage = null)
        }
    }

    fun skipGoal() {
        form.update {
            it.copy(
                selectedTemplate = null,
                customGoalSelected = false,
                customGoalName = "",
                customGoalTargetText = "",
                step = OnboardingStep.Permissions,
                errorMessage = null,
            )
        }
    }

    fun back() {
        form.update { state ->
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
        val s = form.value
        if (s.saving || s.finished) return
        val name = s.jobName.trim()
        val hourly = MoneyFormat.parseMajorToMinor(s.hourlyRateText, s.currencyCode)
        if (name.isEmpty() || hourly == null || hourly <= 0L) {
            form.update {
                it.copy(
                    step = OnboardingStep.CreateJob,
                    errorMessage = "Job name and hourly rate are required",
                )
            }
            return
        }
        viewModelScope.launch {
            form.update { it.copy(saving = true, errorMessage = null) }
            runCatching {
                upsertJob(
                    name = name,
                    hourlyRateMinor = hourly,
                    colorArgb = JobDefaults.DEFAULT_COLOR_ARGB,
                    iconKey = JobDefaults.DEFAULT_ICON_KEY,
                )
                when {
                    s.selectedTemplate != null -> {
                        materializeGoalTemplate(
                            template = s.selectedTemplate,
                            allocationBps = GoalDefaults.DEFAULT_ALLOCATION_BPS,
                        )
                    }
                    s.customGoalSelected -> {
                        val goalName = s.customGoalName.trim()
                        val target = MoneyFormat.parseMajorToMinor(
                            s.customGoalTargetText,
                            s.currencyCode,
                        )
                        require(goalName.isNotEmpty()) { "Name your goal" }
                        require(target != null && target > 0L) { "Enter a valid target amount" }
                        val deadline = GoalMath.deadlineEpochDayFromMonths(clock.now(), 6)
                        upsertGoal(
                            name = goalName,
                            targetMinor = target,
                            deadlineEpochDay = deadline,
                            iconKey = GoalDefaults.DEFAULT_ICON_KEY,
                            colorArgb = GoalDefaults.DEFAULT_COLOR_ARGB,
                            allocationBps = GoalDefaults.DEFAULT_ALLOCATION_BPS,
                        )
                    }
                }
                setOnboardingDone(true)
            }.onSuccess {
                form.update { it.copy(saving = false, finished = true) }
            }.onFailure { err ->
                form.update {
                    it.copy(
                        saving = false,
                        errorMessage = err.message ?: "Couldn’t finish setup",
                    )
                }
            }
        }
    }
}
