package com.trackpay.app.ui.jobs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackpay.app.domain.model.Job
import com.trackpay.app.domain.model.JobDefaults
import com.trackpay.app.domain.usecase.ArchiveJobUseCase
import com.trackpay.app.domain.usecase.GetJobUseCase
import com.trackpay.app.domain.usecase.ListJobsUseCase
import com.trackpay.app.domain.usecase.ObserveCurrencyCodeUseCase
import com.trackpay.app.domain.usecase.UpsertJobUseCase
import com.trackpay.app.location.GeofenceManager
import com.trackpay.app.ui.util.MoneyFormat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class JobsListUiState(
    val jobs: List<Job> = emptyList(),
    val errorMessage: String? = null,
)

data class JobEditorUiState(
    val jobId: String? = null,
    val name: String = "",
    val hourlyRateText: String = "",
    val otRateText: String = "",
    val otThresholdHoursText: String = "8",
    val colorArgb: Int = JobDefaults.DEFAULT_COLOR_ARGB,
    val iconKey: String = JobDefaults.DEFAULT_ICON_KEY,
    val geoEnabled: Boolean = false,
    val latitudeText: String = "",
    val longitudeText: String = "",
    val radiusText: String = JobDefaults.DEFAULT_RADIUS_METERS.toString(),
    val isNew: Boolean = true,
    val errorMessage: String? = null,
    val saved: Boolean = false,
)

@HiltViewModel
class JobsListViewModel @Inject constructor(
    listJobs: ListJobsUseCase,
    private val archiveJob: ArchiveJobUseCase,
) : ViewModel() {
    private val error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<JobsListUiState> = combine(
        listJobs(),
        error,
    ) { jobs, err ->
        JobsListUiState(jobs = jobs, errorMessage = err)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = JobsListUiState(),
    )

    fun archive(jobId: String) {
        viewModelScope.launch {
            runCatching { archiveJob(jobId) }
                .onFailure { error.value = it.message ?: "Archive failed" }
        }
    }
}

@HiltViewModel
class JobEditorViewModel @Inject constructor(
    private val upsertJob: UpsertJobUseCase,
    private val getJob: GetJobUseCase,
    private val geofenceManager: GeofenceManager,
    private val observeCurrencyCode: ObserveCurrencyCodeUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(JobEditorUiState())
    val uiState: StateFlow<JobEditorUiState> = _uiState

    fun load(jobId: String?) {
        if (jobId == null || jobId == "new") {
            _uiState.value = JobEditorUiState(isNew = true)
            return
        }
        viewModelScope.launch {
            val job = getJob(jobId)
            if (job == null) {
                _uiState.value = JobEditorUiState(errorMessage = "Job not found")
            } else {
                _uiState.value = JobEditorUiState(
                    jobId = job.id,
                    name = job.name,
                    hourlyRateText = formatMajor(job.hourlyRateMinor),
                    otRateText = job.otRateMinor?.let { formatMajor(it) }.orEmpty(),
                    otThresholdHoursText = job.otThresholdMinutes
                        ?.let { (it / 60.0).let { h -> if (h % 1.0 == 0.0) h.toInt().toString() else h.toString() } }
                        ?: "8",
                    colorArgb = job.colorArgb,
                    iconKey = job.iconKey,
                    geoEnabled = job.geoEnabled,
                    latitudeText = job.latitude?.toString().orEmpty(),
                    longitudeText = job.longitude?.toString().orEmpty(),
                    radiusText = (job.radiusMeters ?: JobDefaults.DEFAULT_RADIUS_METERS).toString(),
                    isNew = false,
                )
            }
        }
    }

    fun onNameChange(value: String) {
        _uiState.value = _uiState.value.copy(name = value, errorMessage = null)
    }

    fun onHourlyRateChange(value: String) {
        _uiState.value = _uiState.value.copy(hourlyRateText = value, errorMessage = null)
    }

    fun onOtRateChange(value: String) {
        _uiState.value = _uiState.value.copy(otRateText = value, errorMessage = null)
    }

    fun onOtThresholdChange(value: String) {
        _uiState.value = _uiState.value.copy(otThresholdHoursText = value, errorMessage = null)
    }

    fun onColorChange(colorArgb: Int) {
        _uiState.value = _uiState.value.copy(colorArgb = colorArgb, errorMessage = null)
    }

    fun onIconChange(iconKey: String) {
        _uiState.value = _uiState.value.copy(iconKey = iconKey, errorMessage = null)
    }

    fun onGeoEnabledChange(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(geoEnabled = enabled, errorMessage = null)
    }

    fun onLatitudeChange(value: String) {
        _uiState.value = _uiState.value.copy(latitudeText = value, errorMessage = null)
    }

    fun onLongitudeChange(value: String) {
        _uiState.value = _uiState.value.copy(longitudeText = value, errorMessage = null)
    }

    fun onRadiusChange(value: String) {
        _uiState.value = _uiState.value.copy(radiusText = value, errorMessage = null)
    }

    fun save() {
        val s = _uiState.value
        viewModelScope.launch {
            val currency = observeCurrencyCode().first()
            val hourly = MoneyFormat.parseMajorToMinor(s.hourlyRateText, currency)
            if (hourly == null || hourly <= 0L) {
                _uiState.value = s.copy(errorMessage = "Enter a valid hourly rate")
                return@launch
            }
            val otText = s.otRateText.trim()
            val ot = if (otText.isEmpty()) null else MoneyFormat.parseMajorToMinor(otText, currency)
            if (otText.isNotEmpty() && (ot == null || ot <= 0L)) {
                _uiState.value = s.copy(errorMessage = "Enter a valid OT rate or leave blank")
                return@launch
            }
            val thresholdMinutes = if (ot == null) {
                null
            } else {
                val hours = s.otThresholdHoursText.trim().toDoubleOrNull() ?: 8.0
                (hours * 60.0).toInt().coerceAtLeast(1)
            }

            val latitude = s.latitudeText.trim().toDoubleOrNull()
            val longitude = s.longitudeText.trim().toDoubleOrNull()
            val radius = s.radiusText.trim().toIntOrNull()
                ?: JobDefaults.DEFAULT_RADIUS_METERS

            if (s.geoEnabled) {
                if (latitude == null || longitude == null) {
                    _uiState.value = s.copy(errorMessage = "Enter latitude and longitude for location")
                    return@launch
                }
                if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) {
                    _uiState.value = s.copy(errorMessage = "Latitude/longitude out of range")
                    return@launch
                }
                if (radius <= 0) {
                    _uiState.value = s.copy(errorMessage = "Radius must be a positive number of meters")
                    return@launch
                }
            }

            runCatching {
                upsertJob(
                    id = s.jobId,
                    name = s.name,
                    hourlyRateMinor = hourly,
                    otRateMinor = ot,
                    otThresholdMinutes = thresholdMinutes,
                    colorArgb = s.colorArgb,
                    iconKey = s.iconKey,
                    geoEnabled = s.geoEnabled,
                    latitude = if (s.geoEnabled) latitude else null,
                    longitude = if (s.geoEnabled) longitude else null,
                    radiusMeters = if (s.geoEnabled) radius else null,
                    clearGeo = !s.geoEnabled,
                )
                geofenceManager.refresh()
            }.onSuccess {
                _uiState.value = _uiState.value.copy(saved = true, errorMessage = null)
            }.onFailure {
                _uiState.value = _uiState.value.copy(errorMessage = it.message ?: "Save failed")
            }
        }
    }

    private fun formatMajor(minor: Long): String {
        val major = minor / 100.0
        return if (major % 1.0 == 0.0) major.toInt().toString() else major.toString()
    }
}
