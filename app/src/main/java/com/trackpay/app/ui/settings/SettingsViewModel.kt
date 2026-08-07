package com.trackpay.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackpay.app.domain.model.Job
import com.trackpay.app.domain.model.JobDefaults
import com.trackpay.app.domain.usecase.ListJobsUseCase
import com.trackpay.app.domain.usecase.ObserveCurrencyCodeUseCase
import com.trackpay.app.domain.usecase.ObserveGeoMasterEnabledUseCase
import com.trackpay.app.domain.usecase.ObserveLiveNotificationEnabledUseCase
import com.trackpay.app.domain.usecase.SetCurrencyCodeUseCase
import com.trackpay.app.domain.usecase.SetGeoMasterEnabledUseCase
import com.trackpay.app.domain.usecase.SetLiveNotificationEnabledUseCase
import com.trackpay.app.location.GeofenceManager
import com.trackpay.app.ui.util.CurrencyFormat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val currencyCode: String = JobDefaults.DEFAULT_CURRENCY_CODE,
    val currencyCodes: List<String> = CurrencyFormat.COMMON_CODES,
    val geoMasterEnabled: Boolean = true,
    val liveNotificationEnabled: Boolean = true,
    val defaultJobName: String? = null,
    val jobCount: Int = 0,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    listJobs: ListJobsUseCase,
    observeCurrencyCode: ObserveCurrencyCodeUseCase,
    observeGeoMasterEnabled: ObserveGeoMasterEnabledUseCase,
    observeLiveNotificationEnabled: ObserveLiveNotificationEnabledUseCase,
    private val setCurrencyCode: SetCurrencyCodeUseCase,
    private val setGeoMasterEnabled: SetGeoMasterEnabledUseCase,
    private val setLiveNotificationEnabled: SetLiveNotificationEnabledUseCase,
    private val geofenceManager: GeofenceManager,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        observeCurrencyCode(),
        observeGeoMasterEnabled(),
        observeLiveNotificationEnabled(),
        listJobs(),
    ) { currency, geo, liveNotif, jobs ->
        SettingsUiState(
            currencyCode = currency,
            currencyCodes = CurrencyFormat.COMMON_CODES,
            geoMasterEnabled = geo,
            liveNotificationEnabled = liveNotif,
            defaultJobName = resolveDefaultJobName(jobs),
            jobCount = jobs.size,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

    fun onCurrencySelected(code: String) {
        viewModelScope.launch {
            setCurrencyCode(code)
        }
    }

    fun onGeoMasterChanged(enabled: Boolean) {
        viewModelScope.launch {
            setGeoMasterEnabled(enabled)
            geofenceManager.refresh()
        }
    }

    fun onLiveNotificationChanged(enabled: Boolean) {
        viewModelScope.launch {
            setLiveNotificationEnabled(enabled)
        }
    }

    private fun resolveDefaultJobName(jobs: List<Job>): String? =
        jobs.firstOrNull()?.name
}
