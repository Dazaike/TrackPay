package com.trackpay.app.location

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.trackpay.app.MainActivity
import com.trackpay.app.R
import com.trackpay.app.data.local.PreferencesDataSource
import com.trackpay.app.data.repo.JobRepository
import com.trackpay.app.data.repo.SessionRepository
import com.trackpay.app.domain.model.SessionSource
import com.trackpay.app.domain.usecase.ClockInUseCase
import com.trackpay.app.domain.usecase.ClockOutUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Handles Play Services geofence transitions and notification action taps.
 * Default: actionable suggest notification (not silent auto clock).
 */
@AndroidEntryPoint
class GeofenceTransitionReceiver : BroadcastReceiver() {

    @Inject lateinit var jobRepository: JobRepository
    @Inject lateinit var sessionRepository: SessionRepository
    @Inject lateinit var preferences: PreferencesDataSource
    @Inject lateinit var clockIn: ClockInUseCase
    @Inject lateinit var clockOut: ClockOutUseCase

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val pending = goAsync()
        scope.launch {
            try {
                when (intent.action) {
                    ACTION_CLOCK_IN -> handleClockInAction(context, intent)
                    ACTION_CLOCK_OUT -> handleClockOutAction(context, intent)
                    ACTION_TRANSITION, null -> handleTransition(context, intent)
                    else -> handleTransition(context, intent)
                }
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun handleTransition(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return

        val transition = when (event.geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> GeofenceTransitionHandler.Transition.ENTER
            Geofence.GEOFENCE_TRANSITION_EXIT -> GeofenceTransitionHandler.Transition.EXIT
            else -> return
        }

        val masterOn = preferences.geoMasterEnabled.first()
        val active = sessionRepository.getActiveSession()
        val triggering = event.triggeringGeofences.orEmpty()
        for (geofence in triggering) {
            val job = jobRepository.getById(geofence.requestId) ?: continue
            val suggestion = GeofenceTransitionHandler.decide(
                transition = transition,
                job = job,
                activeSession = active,
                geoMasterEnabled = masterOn,
            )
            when (suggestion) {
                is GeofenceTransitionHandler.Suggestion.ClockIn -> {
                    postSuggestNotification(
                        context = context,
                        notificationId = notifIdForJob(suggestion.jobId),
                        title = context.getString(
                            R.string.geofence_enter_title,
                            suggestion.jobName,
                        ),
                        body = context.getString(R.string.geofence_enter_body),
                        actionLabel = context.getString(R.string.action_clock_in),
                        action = ACTION_CLOCK_IN,
                        jobId = suggestion.jobId,
                    )
                }
                is GeofenceTransitionHandler.Suggestion.ClockOut -> {
                    postSuggestNotification(
                        context = context,
                        notificationId = notifIdForJob(suggestion.jobId),
                        title = context.getString(
                            R.string.geofence_exit_title,
                            suggestion.jobName,
                        ),
                        body = context.getString(R.string.geofence_exit_body),
                        actionLabel = context.getString(R.string.action_clock_out),
                        action = ACTION_CLOCK_OUT,
                        jobId = suggestion.jobId,
                    )
                }
                GeofenceTransitionHandler.Suggestion.None -> Unit
            }
        }
    }

    private suspend fun handleClockInAction(context: Context, intent: Intent) {
        val jobId = intent.getStringExtra(EXTRA_JOB_ID) ?: return
        runCatching { clockIn(jobId, SessionSource.GEOFENCE) }
        NotificationManagerCompat.from(context).cancel(notifIdForJob(jobId))
    }

    private suspend fun handleClockOutAction(context: Context, intent: Intent) {
        val jobId = intent.getStringExtra(EXTRA_JOB_ID)
        val active = sessionRepository.getActiveSession()
        if (jobId != null && active != null && active.jobId != jobId) return
        runCatching { clockOut(SessionSource.GEOFENCE) }
        if (jobId != null) {
            NotificationManagerCompat.from(context).cancel(notifIdForJob(jobId))
        }
    }

    private fun postSuggestNotification(
        context: Context,
        notificationId: Int,
        title: String,
        body: String,
        actionLabel: String,
        action: String,
        jobId: String,
    ) {
        if (!canPostNotifications(context)) return
        ensureChannel(context)

        val contentIntent = PendingIntent.getActivity(
            context,
            notificationId,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            pendingFlags(),
        )

        val actionIntent = Intent(context, GeofenceTransitionReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_JOB_ID, jobId)
        }
        val actionPi = PendingIntent.getBroadcast(
            context,
            notificationId + 10_000,
            actionIntent,
            pendingFlags(),
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .addAction(0, actionLabel, actionPi)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notif_channel_geofence_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notif_channel_geofence_desc)
        }
        manager.createNotificationChannel(channel)
    }

    private fun canPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 33) return true
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun pendingFlags(): Int =
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    private fun notifIdForJob(jobId: String): Int =
        NOTIFICATION_BASE + (jobId.hashCode() and 0x0FFF)

    companion object {
        const val ACTION_TRANSITION = "com.trackpay.app.geofence.TRANSITION"
        const val ACTION_CLOCK_IN = "com.trackpay.app.geofence.CLOCK_IN"
        const val ACTION_CLOCK_OUT = "com.trackpay.app.geofence.CLOCK_OUT"
        const val EXTRA_JOB_ID = "job_id"
        const val CHANNEL_ID = "geofence_suggest"
        private const val NOTIFICATION_BASE = 5200
    }
}
