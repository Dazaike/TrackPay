package com.trackpay.app.domain.usecase

import com.trackpay.app.data.local.PreferencesDataSource
import com.trackpay.app.location.GeofenceManager
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ObserveCurrencyCodeUseCase @Inject constructor(
    private val preferences: PreferencesDataSource,
) {
    operator fun invoke(): Flow<String> = preferences.currencyCode
}

@Singleton
class SetCurrencyCodeUseCase @Inject constructor(
    private val preferences: PreferencesDataSource,
) {
    suspend operator fun invoke(code: String) {
        preferences.setCurrencyCode(code)
    }
}

@Singleton
class ObserveOnboardingDoneUseCase @Inject constructor(
    private val preferences: PreferencesDataSource,
) {
    operator fun invoke(): Flow<Boolean> = preferences.onboardingDone
}

@Singleton
class SetOnboardingDoneUseCase @Inject constructor(
    private val preferences: PreferencesDataSource,
) {
    suspend operator fun invoke(done: Boolean = true) {
        preferences.setOnboardingDone(done)
    }
}

@Singleton
class ObserveGeoMasterEnabledUseCase @Inject constructor(
    private val preferences: PreferencesDataSource,
) {
    operator fun invoke(): Flow<Boolean> = preferences.geoMasterEnabled
}

@Singleton
class SetGeoMasterEnabledUseCase @Inject constructor(
    private val preferences: PreferencesDataSource,
    private val geofenceManager: GeofenceManager,
) {
    suspend operator fun invoke(enabled: Boolean) {
        preferences.setGeoMasterEnabled(enabled)
        runCatching { geofenceManager.refresh() }
    }
}

@Singleton
class ObserveLiveNotificationEnabledUseCase @Inject constructor(
    private val preferences: PreferencesDataSource,
) {
    operator fun invoke(): Flow<Boolean> = preferences.liveNotificationEnabled
}

@Singleton
class SetLiveNotificationEnabledUseCase @Inject constructor(
    private val preferences: PreferencesDataSource,
) {
    suspend operator fun invoke(enabled: Boolean) {
        preferences.setLiveNotificationEnabled(enabled)
    }
}

/** Re-register geofences after permission grants or settings changes. */
@Singleton
class RefreshGeofencesUseCase @Inject constructor(
    private val geofenceManager: GeofenceManager,
) {
    suspend operator fun invoke() {
        runCatching { geofenceManager.refresh() }
    }
}
