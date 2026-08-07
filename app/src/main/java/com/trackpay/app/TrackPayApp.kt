package com.trackpay.app

import android.app.Application
import com.trackpay.app.data.repo.SessionRepository
import com.trackpay.app.service.TimerServiceController
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class TrackPayApp : Application() {

    @Inject lateinit var sessionRepository: SessionRepository
    @Inject lateinit var timerServiceController: TimerServiceController

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            val active = sessionRepository.getActiveSession()
            if (active != null) {
                timerServiceController.start()
            }
        }
    }
}
