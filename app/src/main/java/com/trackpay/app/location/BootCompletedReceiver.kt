package com.trackpay.app.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.trackpay.app.data.repo.SessionRepository
import com.trackpay.app.service.TimerServiceController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * On BOOT_COMPLETED, restart the timer FGS if a session is still active.
 */
@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {

    @Inject lateinit var sessionRepository: SessionRepository
    @Inject lateinit var timerServiceController: TimerServiceController
    @Inject lateinit var geofenceManager: GeofenceManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED &&
            intent?.action != Intent.ACTION_LOCKED_BOOT_COMPLETED &&
            intent?.action != "android.intent.action.QUICKBOOT_POWERON"
        ) {
            return
        }
        val pending = goAsync()
        scope.launch {
            try {
                val active = sessionRepository.getActiveSession()
                if (active != null) {
                    timerServiceController.start()
                }
                runCatching { geofenceManager.refresh() }
            } finally {
                pending.finish()
            }
        }
    }
}
