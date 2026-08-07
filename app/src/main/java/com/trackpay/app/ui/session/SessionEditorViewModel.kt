package com.trackpay.app.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackpay.app.domain.model.BreakInput
import com.trackpay.app.domain.model.Job
import com.trackpay.app.domain.time.Clock
import com.trackpay.app.domain.usecase.CreateCompletedSessionUseCase
import com.trackpay.app.domain.usecase.GetSessionDetailUseCase
import com.trackpay.app.domain.usecase.ListJobsUseCase
import com.trackpay.app.domain.usecase.UpdateSessionUseCase
import com.trackpay.app.ui.util.DateTimeFormat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BreakDraft(
    val id: String,
    val startText: String = "",
    val endText: String = "",
)

data class SessionEditorUiState(
    val sessionId: String? = null,
    val isNew: Boolean = true,
    val loading: Boolean = true,
    val jobs: List<Job> = emptyList(),
    val selectedJobId: String? = null,
    val startText: String = "",
    val endText: String = "",
    val notes: String = "",
    val breaks: List<BreakDraft> = emptyList(),
    val applyCurrentJobRates: Boolean = false,
    val errorMessage: String? = null,
    val saved: Boolean = false,
)

@HiltViewModel
class SessionEditorViewModel @Inject constructor(
    private val listJobs: ListJobsUseCase,
    private val getSessionDetail: GetSessionDetailUseCase,
    private val createCompletedSession: CreateCompletedSessionUseCase,
    private val updateSession: UpdateSessionUseCase,
    private val clock: Clock,
) : ViewModel() {

    private val form = MutableStateFlow(SessionEditorUiState())
    private val jobsFlow = listJobs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val uiState: StateFlow<SessionEditorUiState> = combine(form, jobsFlow) { state, jobs ->
        state.copy(jobs = jobs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionEditorUiState())

    fun load(sessionId: String?) {
        val isNew = sessionId.isNullOrBlank() || sessionId == "new"
        if (isNew) {
            viewModelScope.launch {
                val now = clock.now()
                val startDefault = now - DEFAULT_SHIFT_MILLIS
                val jobs = runCatching { listJobs.once() }.getOrDefault(emptyList())
                form.value = SessionEditorUiState(
                    sessionId = null,
                    isNew = true,
                    loading = false,
                    selectedJobId = jobs.firstOrNull()?.id,
                    startText = DateTimeFormat.formatLocalDateTime(startDefault),
                    endText = DateTimeFormat.formatLocalDateTime(now),
                    notes = "",
                    breaks = emptyList(),
                    applyCurrentJobRates = false,
                )
            }
            return
        }

        form.value = SessionEditorUiState(sessionId = sessionId, isNew = false, loading = true)
        viewModelScope.launch {
            runCatching { getSessionDetail(sessionId!!) }
                .onSuccess { detail ->
                    if (detail == null) {
                        form.update {
                            it.copy(loading = false, errorMessage = "Session not found")
                        }
                    } else {
                        val session = detail.session
                        form.update {
                            it.copy(
                                loading = false,
                                sessionId = session.id,
                                isNew = false,
                                selectedJobId = session.jobId,
                                startText = DateTimeFormat.formatLocalDateTime(session.startAt),
                                endText = session.endAt?.let { end ->
                                    DateTimeFormat.formatLocalDateTime(end)
                                }.orEmpty(),
                                notes = session.notes.orEmpty(),
                                breaks = detail.breaks.map { br ->
                                    BreakDraft(
                                        id = br.id,
                                        startText = DateTimeFormat.formatLocalDateTime(br.startAt),
                                        endText = br.endAt?.let { end ->
                                            DateTimeFormat.formatLocalDateTime(end)
                                        }.orEmpty(),
                                    )
                                },
                                applyCurrentJobRates = false,
                                errorMessage = null,
                            )
                        }
                    }
                }
                .onFailure { err ->
                    form.update {
                        it.copy(
                            loading = false,
                            errorMessage = err.message ?: "Failed to load session",
                        )
                    }
                }
        }
    }

    fun onJobSelected(jobId: String) {
        form.update { it.copy(selectedJobId = jobId, errorMessage = null) }
    }

    fun onStartChange(value: String) {
        form.update { it.copy(startText = value, errorMessage = null) }
    }

    fun onEndChange(value: String) {
        form.update { it.copy(endText = value, errorMessage = null) }
    }

    fun onNotesChange(value: String) {
        form.update { it.copy(notes = value, errorMessage = null) }
    }

    fun onApplyCurrentRatesChange(value: Boolean) {
        form.update { it.copy(applyCurrentJobRates = value, errorMessage = null) }
    }

    fun addBreak() {
        form.update { state ->
            val seedStart = DateTimeFormat.parseLocalDateTime(state.startText) ?: clock.now()
            val draft = BreakDraft(
                id = "draft-${clock.now()}-${state.breaks.size}",
                startText = DateTimeFormat.formatLocalDateTime(seedStart),
                endText = DateTimeFormat.formatLocalDateTime(seedStart + DEFAULT_BREAK_MILLIS),
            )
            state.copy(breaks = state.breaks + draft, errorMessage = null)
        }
    }

    fun removeBreak(id: String) {
        form.update { state ->
            state.copy(breaks = state.breaks.filterNot { it.id == id }, errorMessage = null)
        }
    }

    fun onBreakStartChange(id: String, value: String) {
        form.update { state ->
            state.copy(
                breaks = state.breaks.map {
                    if (it.id == id) it.copy(startText = value) else it
                },
                errorMessage = null,
            )
        }
    }

    fun onBreakEndChange(id: String, value: String) {
        form.update { state ->
            state.copy(
                breaks = state.breaks.map {
                    if (it.id == id) it.copy(endText = value) else it
                },
                errorMessage = null,
            )
        }
    }

    fun save() {
        val s = form.value
        val jobId = s.selectedJobId
        if (jobId.isNullOrBlank()) {
            form.update { it.copy(errorMessage = "Select a job") }
            return
        }
        val startAt = DateTimeFormat.parseLocalDateTime(s.startText)
        if (startAt == null) {
            form.update {
                it.copy(errorMessage = "Start must be yyyy-MM-dd HH:mm")
            }
            return
        }
        val endAt = DateTimeFormat.parseLocalDateTime(s.endText)
        if (endAt == null) {
            form.update {
                it.copy(errorMessage = "End must be yyyy-MM-dd HH:mm")
            }
            return
        }
        if (endAt <= startAt) {
            form.update { it.copy(errorMessage = "End must be after start") }
            return
        }

        val breaks = mutableListOf<BreakInput>()
        for ((index, draft) in s.breaks.withIndex()) {
            val bStart = DateTimeFormat.parseLocalDateTime(draft.startText)
            val bEnd = DateTimeFormat.parseLocalDateTime(draft.endText)
            if (bStart == null || bEnd == null) {
                form.update {
                    it.copy(errorMessage = "Break ${index + 1} must be yyyy-MM-dd HH:mm")
                }
                return
            }
            if (bEnd <= bStart) {
                form.update {
                    it.copy(errorMessage = "Break ${index + 1}: end must be after start")
                }
                return
            }
            if (bStart < startAt || bEnd > endAt) {
                form.update {
                    it.copy(errorMessage = "Break ${index + 1} must fall within the session")
                }
                return
            }
            breaks += BreakInput(startAt = bStart, endAt = bEnd)
        }

        val notes = s.notes.trim().ifEmpty { null }
        viewModelScope.launch {
            runCatching {
                if (s.isNew) {
                    createCompletedSession(
                        jobId = jobId,
                        startAt = startAt,
                        endAt = endAt,
                        breaks = breaks,
                        notes = notes,
                    )
                } else {
                    val id = s.sessionId ?: error("Missing session id")
                    updateSession(
                        id = id,
                        jobId = jobId,
                        startAt = startAt,
                        endAt = endAt,
                        breaks = breaks,
                        notes = notes,
                        applyCurrentJobRates = s.applyCurrentJobRates,
                    )
                }
            }.onSuccess {
                form.update { it.copy(saved = true, errorMessage = null) }
            }.onFailure { err ->
                form.update {
                    it.copy(errorMessage = err.message ?: "Save failed")
                }
            }
        }
    }

    companion object {
        private const val DEFAULT_SHIFT_MILLIS = 8L * 60L * 60L * 1_000L
        private const val DEFAULT_BREAK_MILLIS = 30L * 60L * 1_000L
    }
}
