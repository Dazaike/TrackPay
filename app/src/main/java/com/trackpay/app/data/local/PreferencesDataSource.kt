package com.trackpay.app.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.trackpay.app.domain.model.ThemeIds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val data: Flow<Preferences> = dataStore.data

    val lastJobId: Flow<String?> = dataStore.data.map { prefs ->
        prefs[KEY_LAST_JOB_ID]
    }

    val activeThemeId: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_ACTIVE_THEME_ID] ?: ThemeIds.DEFAULT
    }

    val currencyCode: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_CURRENCY_CODE] ?: DEFAULT_CURRENCY_CODE
    }

    val onboardingDone: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING_DONE] ?: false
    }

    val geoMasterEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_GEO_MASTER_ENABLED] ?: true
    }

    val liveNotificationEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_LIVE_NOTIFICATION_ENABLED] ?: true
    }

    suspend fun setActiveThemeId(themeId: String) {
        dataStore.edit { prefs ->
            prefs[KEY_ACTIVE_THEME_ID] = themeId
        }
    }

    suspend fun setLastJobId(jobId: String) {
        dataStore.edit { prefs ->
            prefs[KEY_LAST_JOB_ID] = jobId
        }
    }

    suspend fun setCurrencyCode(code: String) {
        dataStore.edit { prefs ->
            prefs[KEY_CURRENCY_CODE] = code.uppercase()
        }
    }

    suspend fun setOnboardingDone(done: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_DONE] = done
        }
    }

    suspend fun setGeoMasterEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_GEO_MASTER_ENABLED] = enabled
        }
    }

    suspend fun setLiveNotificationEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_LIVE_NOTIFICATION_ENABLED] = enabled
        }
    }

    companion object {
        const val DEFAULT_CURRENCY_CODE: String = "USD"

        private val KEY_LAST_JOB_ID = stringPreferencesKey("last_job_id")
        private val KEY_ACTIVE_THEME_ID = stringPreferencesKey("active_theme_id")
        private val KEY_CURRENCY_CODE = stringPreferencesKey("currency_code")
        private val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        private val KEY_GEO_MASTER_ENABLED = booleanPreferencesKey("geo_master_enabled")
        private val KEY_LIVE_NOTIFICATION_ENABLED = booleanPreferencesKey("live_notification_enabled")
    }
}
