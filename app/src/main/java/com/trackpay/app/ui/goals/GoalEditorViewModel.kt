package com.trackpay.app.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackpay.app.domain.model.GoalDefaults
import com.trackpay.app.domain.model.GoalStatus
import com.trackpay.app.domain.model.GoalTemplate
import com.trackpay.app.domain.usecase.ArchiveGoalUseCase
import com.trackpay.app.domain.usecase.GetGoalUseCase
import com.trackpay.app.domain.usecase.ObserveCurrencyCodeUseCase
import com.trackpay.app.domain.usecase.ObserveGoalProgressUseCase
import com.trackpay.app.domain.usecase.UpsertGoalUseCase
import com.trackpay.app.ui.util.MoneyFormat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import javax.inject.Inject
import kotlin.math.roundToInt

data class GoalEditorUiState(
    val goalId: String? = null,
    val name: String = "",
    val targetText: String = "",
    val deadlineText: String = "",
    val iconKey: String = GoalDefaults.DEFAULT_ICON_KEY,
    val colorArgb: Int = GoalDefaults.DEFAULT_COLOR_ARGB,
    val allocationPercentText: String = "0",
    val allocationPercent: Float = 0f,
    val otherActiveBps: Int = 0,
    val projectedTotalBps: Int = 0,
    val allocationOverLimit: Boolean = false,
    val isNew: Boolean = true,
    val canArchive: Boolean = false,
    val errorMessage: String? = null,
    val saved: Boolean = false,
    val archived: Boolean = false,
)

/** UI-facing icon chips (includes template keys + a few extras). */
object GoalIconPresets {
    val ALL: List<String> = listOf(
        "savings",
        "shield",
        "flight",
        "directions_car",
        "home",
        "laptop",
        "work",
        "star",
    )
}

@HiltViewModel
class GoalEditorViewModel @Inject constructor(
    private val upsertGoal: UpsertGoalUseCase,
    private val getGoal: GetGoalUseCase,
    private val archiveGoal: ArchiveGoalUseCase,
    private val observeCurrencyCode: ObserveCurrencyCodeUseCase,
    observeGoalProgress: ObserveGoalProgressUseCase,
) : ViewModel() {

    private val form = MutableStateFlow(GoalEditorUiState())

    val uiState: StateFlow<GoalEditorUiState> = combine(
        form,
        observeGoalProgress(),
    ) { state, goals ->
        val otherBps = goals
            .asSequence()
            .filter { it.goal.status == GoalStatus.ACTIVE }
            .filter { it.goal.id != state.goalId }
            .sumOf { it.goal.allocationBps }
        val ownBps = percentToBps(state.allocationPercent)
        val total = otherBps + ownBps
        state.copy(
            otherActiveBps = otherBps,
            projectedTotalBps = total,
            allocationOverLimit = total > GoalDefaults.BPS_DENOMINATOR,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = GoalEditorUiState(),
    )

    fun load(goalId: String?) {
        if (goalId == null || goalId == "new") {
            form.value = defaultNewState()
            return
        }
        viewModelScope.launch {
            val goal = getGoal(goalId)
            if (goal == null) {
                form.value = GoalEditorUiState(errorMessage = "Goal not found")
            } else {
                form.value = GoalEditorUiState(
                    goalId = goal.id,
                    name = goal.name,
                    targetText = formatMajor(goal.targetMinor),
                    deadlineText = epochDayToText(goal.deadlineEpochDay),
                    iconKey = goal.iconKey,
                    colorArgb = goal.colorArgb,
                    allocationPercentText = bpsToPercentText(goal.allocationBps),
                    allocationPercent = bpsToPercent(goal.allocationBps),
                    isNew = false,
                    canArchive = goal.status == GoalStatus.ACTIVE || goal.status == GoalStatus.COMPLETED,
                )
            }
        }
    }

    fun applyTemplate(template: GoalTemplate) {
        val deadline = LocalDate.now(ZoneId.systemDefault())
            .plusMonths(template.defaultHorizonMonths.toLong())
        form.value = GoalEditorUiState(
            isNew = true,
            name = template.name,
            targetText = formatMajor(template.defaultTargetMinor),
            deadlineText = deadline.format(DATE_FMT),
            iconKey = template.iconKey,
            colorArgb = template.colorArgb,
            allocationPercentText = "0",
            allocationPercent = 0f,
        )
    }

    fun onNameChange(value: String) {
        form.value = form.value.copy(name = value, errorMessage = null)
    }

    fun onTargetChange(value: String) {
        form.value = form.value.copy(targetText = value, errorMessage = null)
    }

    fun onDeadlineChange(value: String) {
        form.value = form.value.copy(deadlineText = value, errorMessage = null)
    }

    fun onIconChange(iconKey: String) {
        form.value = form.value.copy(iconKey = iconKey, errorMessage = null)
    }

    fun onColorChange(colorArgb: Int) {
        form.value = form.value.copy(colorArgb = colorArgb, errorMessage = null)
    }

    fun onAllocationPercentTextChange(value: String) {
        val cleaned = value.filter { it.isDigit() || it == '.' || it == ',' }.replace(',', '.')
        val percent = cleaned.toFloatOrNull()?.coerceIn(0f, 100f)
        form.value = form.value.copy(
            allocationPercentText = cleaned,
            allocationPercent = percent ?: form.value.allocationPercent,
            errorMessage = null,
        )
    }

    fun onAllocationSliderChange(percent: Float) {
        val clamped = percent.coerceIn(0f, 100f)
        form.value = form.value.copy(
            allocationPercent = clamped,
            allocationPercentText = formatPercent(clamped),
            errorMessage = null,
        )
    }

    fun save() {
        val s = form.value
        val name = s.name.trim()
        if (name.isEmpty()) {
            form.value = s.copy(errorMessage = "Enter a goal name")
            return
        }
        val targetText = s.targetText
        // parsed inside coroutine with active currency
        if (targetText.isBlank()) {
            form.value = s.copy(errorMessage = "Enter a valid target amount")
            return
        }
        val deadlineDay = parseDeadlineEpochDay(s.deadlineText)
        if (deadlineDay == null) {
            form.value = s.copy(errorMessage = "Deadline must be yyyy-MM-dd")
            return
        }
        val bps = percentToBps(s.allocationPercent)
        if (bps !in 0..GoalDefaults.BPS_DENOMINATOR) {
            form.value = s.copy(errorMessage = "Allocation must be between 0% and 100%")
            return
        }
        if (s.otherActiveBps + bps > GoalDefaults.BPS_DENOMINATOR) {
            form.value = s.copy(
                errorMessage = "Total allocation across active goals cannot exceed 100%",
            )
            return
        }
        viewModelScope.launch {
            val currency = observeCurrencyCode().first()
            val target = MoneyFormat.parseMajorToMinor(targetText, currency)
            if (target == null || target <= 0L) {
                form.value = s.copy(errorMessage = "Enter a valid target amount")
                return@launch
            }
            runCatching {
                upsertGoal(
                    id = s.goalId,
                    name = name,
                    targetMinor = target,
                    deadlineEpochDay = deadlineDay,
                    iconKey = s.iconKey,
                    colorArgb = s.colorArgb,
                    allocationBps = bps,
                )
            }.onSuccess {
                form.value = form.value.copy(saved = true, errorMessage = null)
            }.onFailure {
                form.value = form.value.copy(errorMessage = it.message ?: "Save failed")
            }
        }
    }

    fun archive() {
        val id = form.value.goalId ?: return
        viewModelScope.launch {
            runCatching { archiveGoal(id) }
                .onSuccess {
                    form.value = form.value.copy(archived = true, errorMessage = null)
                }
                .onFailure {
                    form.value = form.value.copy(errorMessage = it.message ?: "Archive failed")
                }
        }
    }

    private fun defaultNewState(): GoalEditorUiState {
        val defaultDeadline = LocalDate.now(ZoneId.systemDefault()).plusMonths(6)
        return GoalEditorUiState(
            isNew = true,
            deadlineText = defaultDeadline.format(DATE_FMT),
            iconKey = GoalDefaults.DEFAULT_ICON_KEY,
            colorArgb = GoalDefaults.DEFAULT_COLOR_ARGB,
            allocationPercentText = "0",
            allocationPercent = 0f,
        )
    }

    private fun formatMajor(minor: Long): String {
        val major = minor / 100.0
        return if (major % 1.0 == 0.0) major.toInt().toString() else major.toString()
    }

    private fun epochDayToText(epochDay: Long): String =
        LocalDate.ofEpochDay(epochDay).format(DATE_FMT)

    private fun parseDeadlineEpochDay(text: String): Long? {
        return try {
            LocalDate.parse(text.trim(), DATE_FMT).toEpochDay()
        } catch (_: DateTimeParseException) {
            null
        }
    }

    companion object {
        private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

        fun percentToBps(percent: Float): Int =
            (percent.coerceIn(0f, 100f) * 100f).roundToInt()
                .coerceIn(0, GoalDefaults.BPS_DENOMINATOR)

        fun bpsToPercent(bps: Int): Float =
            (bps.coerceIn(0, GoalDefaults.BPS_DENOMINATOR) / 100f)

        fun bpsToPercentText(bps: Int): String = formatPercent(bpsToPercent(bps))

        fun formatPercent(percent: Float): String {
            val rounded = (percent * 10f).roundToInt() / 10f
            return if (rounded % 1f == 0f) rounded.toInt().toString() else rounded.toString()
        }
    }
}
