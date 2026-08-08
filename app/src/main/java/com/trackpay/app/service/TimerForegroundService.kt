package com.trackpay.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.trackpay.app.MainActivity
import com.trackpay.app.R
import com.trackpay.app.data.local.PreferencesDataSource
import com.trackpay.app.data.repo.SessionRepository
import com.trackpay.app.domain.calc.EarningsCalculator
import com.trackpay.app.domain.model.ActiveSession
import com.trackpay.app.domain.model.JobDefaults
import com.trackpay.app.domain.model.SessionSource
import com.trackpay.app.domain.model.SessionStatus
import com.trackpay.app.domain.time.Clock
import com.trackpay.app.domain.usecase.ClockOutUseCase
import com.trackpay.app.domain.usecase.PauseSessionUseCase
import com.trackpay.app.domain.usecase.ResumeSessionUseCase
import com.trackpay.app.ui.util.MoneyFormat
import com.trackpay.app.ui.util.TimeFormat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TimerForegroundService : Service() {

    @Inject lateinit var sessionRepository: SessionRepository
    @Inject lateinit var preferences: PreferencesDataSource
    @Inject lateinit var clock: Clock
    @Inject lateinit var pauseSession: PauseSessionUseCase
    @Inject lateinit var resumeSession: ResumeSessionUseCase
    @Inject lateinit var clockOut: ClockOutUseCase

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var tickerJob: Job? = null
    private var observeJob: Job? = null
    private var latestActive: ActiveSession? = null

    @Volatile
    private var currencyCode: String = JobDefaults.DEFAULT_CURRENCY_CODE

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
        observeJob = scope.launch {
            launch {
                preferences.currencyCode.collectLatest { code ->
                    currencyCode = code
                    latestActive?.let { postNotification(it) }
                }
            }
            sessionRepository.observeActiveSession().collectLatest { active ->
                latestActive = active
                if (active == null) {
                    stopSelfSafe()
                } else {
                    postNotification(active)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> scope.launch {
                runCatching { pauseSession(SessionSource.NOTIFICATION) }
            }
            ACTION_RESUME -> scope.launch {
                runCatching { resumeSession(SessionSource.NOTIFICATION) }
            }
            ACTION_CLOCK_OUT -> scope.launch {
                runCatching { clockOut(SessionSource.NOTIFICATION) }
            }
            ACTION_STOP -> {
                stopForegroundCompat()
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                // START / REFRESH / null (system restart)
                promoteToForeground()
                startTicker()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        tickerJob?.cancel()
        observeJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    private fun promoteToForeground() {
        val notification = buildNotification(latestActive)
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startTicker() {
        if (tickerJob?.isActive == true) return
        tickerJob = scope.launch {
            while (isActive) {
                val active = latestActive
                if (active != null) {
                    postNotification(active)
                }
                delay(1_000L)
            }
        }
    }

    private fun postNotification(active: ActiveSession) {
        val notification = buildNotification(active)
        if (!canPostNotifications()) {
            // Session still runs; notification suppressed when permission denied.
            promoteToForegroundSilent(notification)
            return
        }
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun promoteToForegroundSilent(notification: Notification) {
        // Still required to be in foreground while active; content may be minimal.
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(active: ActiveSession?): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            pendingFlags(),
        )

        if (active == null) {
            return NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(getString(R.string.notif_session_title_idle))
                .setContentText(getString(R.string.notif_session_idle_body))
                .setContentIntent(contentIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
        }

        val now = clock.now()
        val earnings = EarningsCalculator.calculate(
            session = active.session,
            breaks = active.breaks,
            nowMillis = now,
        )
        val elapsed = TimeFormat.formatElapsed(earnings.activeMillis)
        val money = MoneyFormat.format(earnings.earnedMinor, currencyCode = currencyCode)
        val statusLabel = when (active.session.status) {
            SessionStatus.PAUSED -> getString(R.string.notif_status_paused)
            else -> getString(R.string.notif_status_running)
        }
        val title = "${active.job.name} · $statusLabel"
        val body = "$elapsed · $money"

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setSubText(
                MoneyFormat.formatRate(
                    active.session.snapshotHourlyRateMinor,
                    currencyCode = currencyCode,
                ),
            )
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setShowWhen(false)
            .setUsesChronometer(false)

        when (active.session.status) {
            SessionStatus.RUNNING -> {
                builder.addAction(
                    0,
                    getString(R.string.action_pause),
                    actionPendingIntent(ACTION_PAUSE, 1),
                )
            }
            SessionStatus.PAUSED -> {
                builder.addAction(
                    0,
                    getString(R.string.action_resume),
                    actionPendingIntent(ACTION_RESUME, 2),
                )
            }
            else -> Unit
        }
        builder.addAction(
            0,
            getString(R.string.action_clock_out),
            actionPendingIntent(ACTION_CLOCK_OUT, 3),
        )

        return builder.build()
    }

    private fun actionPendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, TimerForegroundService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(this, requestCode, intent, pendingFlags())
    }

    private fun pendingFlags(): Int {
        return PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notif_channel_session_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.notif_channel_session_desc)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun canPostNotifications(): Boolean {
        if (Build.VERSION.SDK_INT < 33) return true
        return ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID)
    }

    private fun stopSelfSafe() {
        stopForegroundCompat()
        stopSelf()
    }

    companion object {
        const val CHANNEL_ID = "session_live"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.trackpay.app.timer.START"
        const val ACTION_REFRESH = "com.trackpay.app.timer.REFRESH"
        const val ACTION_STOP = "com.trackpay.app.timer.STOP"
        const val ACTION_PAUSE = "com.trackpay.app.timer.PAUSE"
        const val ACTION_RESUME = "com.trackpay.app.timer.RESUME"
        const val ACTION_CLOCK_OUT = "com.trackpay.app.timer.CLOCK_OUT"
    }
}
