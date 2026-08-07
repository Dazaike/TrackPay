package com.trackpay.app.location

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.trackpay.app.data.local.PreferencesDataSource
import com.trackpay.app.data.repo.JobRepository
import com.trackpay.app.domain.model.Job
import com.trackpay.app.domain.model.JobDefaults
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registers Play Services geofences for jobs with geo enabled.
 * Gracefully no-ops when Play Services or location permission is missing.
 */
@Singleton
class GeofenceManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val jobRepository: JobRepository,
    private val preferences: PreferencesDataSource,
) {
    private val client: GeofencingClient by lazy {
        LocationServices.getGeofencingClient(context)
    }

    fun isPlayServicesAvailable(): Boolean {
        val code = GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(context)
        return code == ConnectionResult.SUCCESS
    }

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    /**
     * Reconcile registered geofences with current jobs + master switch.
     * Safe to call from app start, settings changes, or job edits.
     */
    suspend fun refresh() {
        if (!isPlayServicesAvailable() || !hasLocationPermission()) {
            runCatching { removeAll() }
            return
        }
        val masterOn = preferences.geoMasterEnabled.first()
        if (!masterOn) {
            runCatching { removeAll() }
            return
        }
        val jobs = jobRepository.listActiveJobs().filter { it.isGeofenceReady() }
        if (jobs.isEmpty()) {
            runCatching { removeAll() }
            return
        }
        register(jobs)
    }

    fun pendingIntent(): PendingIntent {
        val intent = Intent(context, GeofenceTransitionReceiver::class.java).apply {
            action = GeofenceTransitionReceiver.ACTION_TRANSITION
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags)
    }

    private fun register(jobs: List<Job>) {
        val geofences = jobs.mapNotNull { job ->
            val lat = job.latitude ?: return@mapNotNull null
            val lng = job.longitude ?: return@mapNotNull null
            val radius = (job.radiusMeters ?: JobDefaults.DEFAULT_RADIUS_METERS)
                .toFloat()
                .coerceIn(MIN_RADIUS_METERS, MAX_RADIUS_METERS)
            Geofence.Builder()
                .setRequestId(job.id)
                .setCircularRegion(lat, lng, radius)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(
                    Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT,
                )
                .setLoiteringDelay(0)
                .build()
        }
        if (geofences.isEmpty()) return

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofences)
            .build()

        // Remove then add to avoid stale request ids for edited jobs.
        client.removeGeofences(pendingIntent()).addOnCompleteListener {
            runCatching {
                client.addGeofences(request, pendingIntent())
            }
        }
    }

    private fun removeAll() {
        client.removeGeofences(pendingIntent())
    }

    private fun Job.isGeofenceReady(): Boolean =
        geoEnabled &&
            !archived &&
            latitude != null &&
            longitude != null &&
            (radiusMeters == null || radiusMeters > 0)

    companion object {
        private const val REQUEST_CODE = 4401
        private const val MIN_RADIUS_METERS = 50f
        private const val MAX_RADIUS_METERS = 2000f
    }
}
