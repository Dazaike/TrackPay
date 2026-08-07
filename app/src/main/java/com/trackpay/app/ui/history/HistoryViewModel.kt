package com.trackpay.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackpay.app.data.repo.SessionRepository
import com.trackpay.app.domain.model.HistoryFilter
import com.trackpay.app.domain.model.Job
import com.trackpay.app.domain.model.SessionListItem
import com.trackpay.app.domain.model.SessionTotals
import com.trackpay.app.domain.time.Clock
import com.trackpay.app.domain.usecase.ListJobsUseCase
import com.trackpay.app.domain.usecase.ObserveHistoryUseCase
import com.trackpay.app.domain.usecase.computeSessionTotals
import com.trackpay.app.ui.util.TimeFormat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

enum class HistoryRangePreset {
    DAYS_7,
    DAYS_30,
    DAYS_90,
    ALL,
}

data class HistoryDayGroup(
    val dayStartMillis: Long,
    val label: String,
    val items: List<SessionListItem>,
)

data class HistoryUiState(
    val loading: Boolean = true,
    val jobs: List<Job> = emptyList(),
    val groups: List<HistoryDayGroup> = emptyList(),
    val totals: SessionTotals = SessionTotals(0L, 0, 0L),
    val query: String = "",
    val selectedJobId: String? = null,
    val rangePreset: HistoryRangePreset = HistoryRangePreset.ALL,
    val hasAnySessions: Boolean = false,
    val filtersActive: Boolean = false,
)

private data class HistoryFilterUi(
    val queryRaw: String = "",
    val debouncedQuery: String = "",
    val jobId: String? = null,
    val rangePreset: HistoryRangePreset = HistoryRangePreset.ALL,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val observeHistory: ObserveHistoryUseCase,
    listJobs: ListJobsUseCase,
    sessionRepository: SessionRepository,
    private val clock: Clock,
) : ViewModel() {

    private val queryInput = MutableStateFlow("")
    private val selectedJobId = MutableStateFlow<String?>(null)
    private val rangePreset = MutableStateFlow(HistoryRangePreset.ALL)

    private val debouncedQuery = queryInput
        .debounce(SEARCH_DEBOUNCE_MS)
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    private val filterUi: StateFlow<HistoryFilterUi> = combine(
        queryInput,
        debouncedQuery,
        selectedJobId,
        rangePreset,
    ) { queryRaw, debounced, jobId, preset ->
        HistoryFilterUi(
            queryRaw = queryRaw,
            debouncedQuery = debounced,
            jobId = jobId,
            rangePreset = preset,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryFilterUi())

    private val historyFilter: StateFlow<HistoryFilter> = filterUi
        .map { ui ->
            val (start, end) = rangeBounds(ui.rangePreset, clock.now())
            HistoryFilter(
                query = ui.debouncedQuery,
                jobId = ui.jobId,
                rangeStartMillis = start,
                rangeEndExclusiveMillis = end,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryFilter())

    private val filteredItems = historyFilter.flatMapLatest { filter ->
        observeHistory(filter)
    }

    private val hasAnySessions = sessionRepository.observeCompletedSessions()
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val uiState: StateFlow<HistoryUiState> = combine(
        filteredItems,
        listJobs(),
        filterUi,
        hasAnySessions,
    ) { items, jobs, filters, anySessions ->
        val totals = computeSessionTotals(items)
        val filtersActive = filters.queryRaw.isNotBlank() ||
            filters.jobId != null ||
            filters.rangePreset != HistoryRangePreset.ALL

        HistoryUiState(
            loading = false,
            jobs = jobs,
            groups = groupByLocalDay(items),
            totals = totals,
            query = filters.queryRaw,
            selectedJobId = filters.jobId,
            rangePreset = filters.rangePreset,
            hasAnySessions = anySessions,
            filtersActive = filtersActive,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HistoryUiState(),
    )

    fun onQueryChange(value: String) {
        queryInput.value = value
    }

    fun onJobFilter(jobId: String?) {
        selectedJobId.value = jobId
    }

    fun onRangePreset(preset: HistoryRangePreset) {
        rangePreset.value = preset
    }

    fun clearFilters() {
        queryInput.value = ""
        selectedJobId.value = null
        rangePreset.value = HistoryRangePreset.ALL
    }

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 300L

        private val dayHeaderFormatter: DateTimeFormatter =
            DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.getDefault())

        fun rangeBounds(
            preset: HistoryRangePreset,
            nowMillis: Long,
            zoneId: ZoneId = ZoneId.systemDefault(),
        ): Pair<Long?, Long?> {
            if (preset == HistoryRangePreset.ALL) return null to null
            val todayStart = TimeFormat.startOfLocalDayMillis(nowMillis, zoneId)
            val days = when (preset) {
                HistoryRangePreset.DAYS_7 -> 7L
                HistoryRangePreset.DAYS_30 -> 30L
                HistoryRangePreset.DAYS_90 -> 90L
                HistoryRangePreset.ALL -> return null to null
            }
            val start = Instant.ofEpochMilli(todayStart)
                .atZone(zoneId)
                .toLocalDate()
                .minusDays(days - 1)
                .atStartOfDay(zoneId)
                .toInstant()
                .toEpochMilli()
            val endExclusive = Instant.ofEpochMilli(todayStart)
                .atZone(zoneId)
                .toLocalDate()
                .plusDays(1)
                .atStartOfDay(zoneId)
                .toInstant()
                .toEpochMilli()
            return start to endExclusive
        }

        fun groupByLocalDay(
            items: List<SessionListItem>,
            zoneId: ZoneId = ZoneId.systemDefault(),
        ): List<HistoryDayGroup> {
            if (items.isEmpty()) return emptyList()
            val grouped = linkedMapOf<Long, MutableList<SessionListItem>>()
            for (item in items) {
                val dayStart = TimeFormat.startOfLocalDayMillis(item.session.startAt, zoneId)
                grouped.getOrPut(dayStart) { mutableListOf() }.add(item)
            }
            return grouped.map { (dayStart, dayItems) ->
                val label = Instant.ofEpochMilli(dayStart)
                    .atZone(zoneId)
                    .toLocalDate()
                    .format(dayHeaderFormatter)
                HistoryDayGroup(
                    dayStartMillis = dayStart,
                    label = label,
                    items = dayItems,
                )
            }
        }
    }
}
