package com.trackpay.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackpay.app.data.local.PreferencesDataSource
import com.trackpay.app.data.repo.SessionRepository
import com.trackpay.app.domain.calc.EarningsCalculator
import com.trackpay.app.domain.model.ActiveSession
import com.trackpay.app.domain.model.GoalProgress
import com.trackpay.app.domain.model.Job
import com.trackpay.app.domain.model.SessionStatus
import com.trackpay.app.domain.model.WorkSession
import com.trackpay.app.domain.time.Clock
import com.trackpay.app.domain.usecase.ClockInUseCase
import com.trackpay.app.domain.usecase.ClockOutUseCase
import com.trackpay.app.domain.usecase.ListJobsUseCase
import com.trackpay.app.domain.usecase.ObserveGoalProgressUseCase
import com.trackpay.app.domain.usecase.ObserveStreakUseCase
import com.trackpay.app.domain.usecase.PauseSessionUseCase
import com.trackpay.app.domain.usecase.ResumeSessionUseCase
import com.trackpay.app.ui.util.TimeFormat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val loading: Boolean = true,
    val jobs: List<Job> = emptyList(),
    val selectedJobId: String? = null,
    val active: ActiveSession? = null,
    val earnedMinor: Long = 0L,
    val activeMillis: Long = 0L,
    val todayEarnedMinor: Long = 0L,
    val weekEarnedMinor: Long = 0L,
    val topGoal: GoalProgress? = null,
    val streakCurrentDays: Int = 0,
    val errorMessage: String? = null,
) {
    val isRunning: Boolean get() = active?.session?.status == SessionStatus.RUNNING
    val isPaused: Boolean get() = active?.session?.status == SessionStatus.PAUSED
    val hasActive: Boolean get() = active != null
    val selectedJob: Job?
        get() = jobs.firstOrNull { it.id == selectedJobId } ?: jobs.firstOrNull()
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    listJobs: ListJobsUseCase,
    observeGoalProgress: ObserveGoalProgressUseCase,
    observeStreak: ObserveStreakUseCase,
    private val sessionRepository: SessionRepository,
    private val preferences: PreferencesDataSource,
    private val clockIn: ClockInUseCase,
    private val clockOut: ClockOutUseCase,
    private val pauseSession: PauseSessionUseCase,
    private val resumeSession: ResumeSessionUseCase,
    private val clock: Clock,
) : ViewModel() {

    private val error = MutableStateFlow<String?>(null)
    private val selectedJobOverride = MutableStateFlow<String?>(null)

    private val nowFlow = flow {
        while (true) {
            emit(clock.now())
            delay(TimeFormat.millisUntilNextSecond(clock.now()).coerceAtLeast(50L))
        }
    }

    private data class BaseSnapshot(
        val jobs: List<Job>,
        val active: ActiveSession?,
        val lastJobId: String?,
        val overrideId: String?,
        val topGoal: GoalProgress?,
        val streakCurrentDays: Int,
        val errorMessage: String?,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DashboardUiState> = combine(
        combine(
            listJobs(),
            sessionRepository.observeActiveSession(),
            preferences.lastJobId,
            selectedJobOverride,
            error,
        ) { jobs, active, lastJobId, overrideId, err ->
            BaseSnapshot(
                jobs = jobs,
                active = active,
                lastJobId = lastJobId,
                overrideId = overrideId,
                topGoal = null,
                streakCurrentDays = 0,
                errorMessage = err,
            )
        },
        observeGoalProgress(),
        observeStreak(),
    ) { base, goals, streak ->
        base.copy(
            topGoal = goals.firstOrNull(),
            streakCurrentDays = streak.currentDays,
        )
    }.flatMapLatest { base ->
        val tickSource = if (base.active != null) {
            nowFlow
        } else {
            flow { emit(clock.now()) }
        }
        tickSource.map { now ->
            val active = base.active
            val breakdown = if (active != null) {
                EarningsCalculator.calculate(active.session, active.breaks, now)
            } else {
                null
            }
            val liveEarned = breakdown?.earnedMinor ?: 0L
            val period = computePeriodEarnings(now, active, liveEarned)
            val selected = if (active != null) {
                active.job.id
            } else {
                resolveSelectedJobId(base.jobs, base.lastJobId, base.overrideId)
            }
            DashboardUiState(
                loading = false,
                jobs = base.jobs,
                selectedJobId = selected,
                active = active,
                earnedMinor = liveEarned,
                activeMillis = breakdown?.activeMillis ?: 0L,
                todayEarnedMinor = period.first,
                weekEarnedMinor = period.second,
                topGoal = base.topGoal,
                streakCurrentDays = base.streakCurrentDays,
                errorMessage = base.errorMessage,
            )
        }
    }.distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DashboardUiState(),
        )

    fun selectJob(jobId: String) {
        if (uiState.value.hasActive) return
        selectedJobOverride.value = jobId
        viewModelScope.launch { preferences.setLastJobId(jobId) }
    }

    fun onClockIn() {
        val jobId = uiState.value.selectedJob?.id ?: return
        viewModelScope.launch {
            runCatching { clockIn(jobId) }
                .onFailure { error.value = it.message ?: "Could not clock in" }
                .onSuccess { error.value = null }
        }
    }

    fun onPause() {
        viewModelScope.launch {
            runCatching { pauseSession() }
                .onFailure { error.value = it.message ?: "Could not pause" }
                .onSuccess { error.value = null }
        }
    }

    fun onResume() {
        viewModelScope.launch {
            runCatching { resumeSession() }
                .onFailure { error.value = it.message ?: "Could not resume" }
                .onSuccess { error.value = null }
        }
    }

    fun onClockOut() {
        viewModelScope.launch {
            runCatching { clockOut() }
                .onFailure { error.value = it.message ?: "Could not clock out" }
                .onSuccess { error.value = null }
        }
    }

    fun dismissError() {
        error.value = null
    }

    private fun resolveSelectedJobId(
        jobs: List<Job>,
        lastJobId: String?,
        overrideId: String?,
    ): String? {
        if (jobs.isEmpty()) return null
        overrideId?.let { id -> if (jobs.any { it.id == id }) return id }
        lastJobId?.let { id -> if (jobs.any { it.id == id }) return id }
        return jobs.first().id
    }

    private suspend fun computePeriodEarnings(
        now: Long,
        active: ActiveSession?,
        liveEarned: Long,
    ): Pair<Long, Long> {
        val dayStart = TimeFormat.startOfLocalDayMillis(now)
        val weekStart = TimeFormat.startOfLocalWeekMillis(now)
        val completedToday = sessionRepository.listCompletedBetween(dayStart, now + 1)
        val completedWeek = sessionRepository.listCompletedBetween(weekStart, now + 1)

        suspend fun sumCompleted(sessions: List<WorkSession>): Long {
            var total = 0L
            for (s in sessions) {
                val end = s.endAt ?: continue
                val breaks = sessionRepository.listBreaks(s.id)
                total += EarningsCalculator.calculate(s, breaks, end).earnedMinor
            }
            return total
        }

        var today = sumCompleted(completedToday)
        var week = sumCompleted(completedWeek)
        if (active != null) {
            if (active.session.startAt >= dayStart) today += liveEarned
            if (active.session.startAt >= weekStart) week += liveEarned
        }
        return today to week
    }
}
