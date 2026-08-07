package com.trackpay.app.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
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

    suspend fun setLastJobId(jobId: String) {
        dataStore.edit { prefs ->
            prefs[KEY_LAST_JOB_ID] = jobId
        }
    }

    private companion object {
        val KEY_LAST_JOB_ID = stringPreferencesKey("last_job_id")
    }
}
