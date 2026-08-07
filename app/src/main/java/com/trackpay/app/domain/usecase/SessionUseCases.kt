package com.trackpay.app.domain.usecase

import com.trackpay.app.data.local.PreferencesDataSource
import com.trackpay.app.data.repo.JobRepository
import com.trackpay.app.data.repo.SessionRepository
import com.trackpay.app.domain.model.BreakInterval
import com.trackpay.app.domain.model.JobDefaults
import com.trackpay.app.domain.model.SessionSource
import com.trackpay.app.domain.model.SessionStatus
import com.trackpay.app.domain.model.WorkSession
import com.trackpay.app.domain.time.Clock
import com.trackpay.app.service.TimerServiceController
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

class ActiveSessionExistsException : IllegalStateException("An active session already exists")
class NoActiveSessionException : IllegalStateException("No active session")
class JobNotFoundException(id: String) : IllegalArgumentException("Job not found: $id")
class InvalidSessionStateException(message: String) : IllegalStateException(message)

@Singleton
class ClockInUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val jobRepository: JobRepository,
    private val preferences: PreferencesDataSource,
    private val clock: Clock,
    private val timerServiceController: TimerServiceController,
) {
    suspend operator fun invoke(
        jobId: String,
        source: SessionSource = SessionSource.MANUAL,
    ): WorkSession {
        if (sessionRepository.getActiveSession() != null) {
            throw ActiveSessionExistsException()
        }
        val job = jobRepository.getById(jobId) ?: throw JobNotFoundException(jobId)
        if (job.archived) throw JobNotFoundException(jobId)

        val now = clock.now()
        val session = WorkSession(
            id = UUID.randomUUID().toString(),
            jobId = job.id,
            startAt = now,
            endAt = null,
            status = SessionStatus.RUNNING,
            snapshotHourlyRateMinor = job.hourlyRateMinor,
            snapshotOtRateMinor = job.otRateMinor,
            snapshotOtThresholdMinutes = when {
                job.otRateMinor == null -> null
                else -> job.otThresholdMinutes ?: JobDefaults.DEFAULT_OT_THRESHOLD_MINUTES
            },
            notes = null,
            source = source,
        )
        sessionRepository.insert(session)
        preferences.setLastJobId(job.id)
        timerServiceController.start()
        return session
    }
}

@Singleton
class PauseSessionUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val clock: Clock,
    private val timerServiceController: TimerServiceController,
) {
    suspend operator fun invoke(source: SessionSource = SessionSource.MANUAL) {
        val active = sessionRepository.getActiveSession() ?: throw NoActiveSessionException()
        if (active.status != SessionStatus.RUNNING) {
            throw InvalidSessionStateException("Session is not running")
        }
        val now = clock.now()
        sessionRepository.update(active.copy(status = SessionStatus.PAUSED))
        sessionRepository.insertBreak(
            BreakInterval(
                id = UUID.randomUUID().toString(),
                sessionId = active.id,
                startAt = now,
                endAt = null,
            ),
        )
        timerServiceController.refresh()
    }
}

@Singleton
class ResumeSessionUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val clock: Clock,
    private val timerServiceController: TimerServiceController,
) {
    suspend operator fun invoke(source: SessionSource = SessionSource.MANUAL) {
        val active = sessionRepository.getActiveSession() ?: throw NoActiveSessionException()
        if (active.status != SessionStatus.PAUSED) {
            throw InvalidSessionStateException("Session is not paused")
        }
        val openBreak = sessionRepository.getOpenBreak(active.id)
            ?: throw InvalidSessionStateException("Missing open break for paused session")
        val now = clock.now()
        sessionRepository.updateBreak(openBreak.copy(endAt = now))
        sessionRepository.update(active.copy(status = SessionStatus.RUNNING))
        timerServiceController.start()
    }
}

@Singleton
class ClockOutUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val clock: Clock,
    private val timerServiceController: TimerServiceController,
) {
    /**
     * Completes the active session. Phase 3 will hook AllocateSession here.
     */
    suspend operator fun invoke(source: SessionSource = SessionSource.MANUAL): WorkSession {
        val active = sessionRepository.getActiveSession() ?: throw NoActiveSessionException()
        if (active.status != SessionStatus.RUNNING && active.status != SessionStatus.PAUSED) {
            throw InvalidSessionStateException("Session is not active")
        }
        val now = clock.now()
        val openBreak = sessionRepository.getOpenBreak(active.id)
        if (openBreak != null) {
            sessionRepository.updateBreak(openBreak.copy(endAt = now))
        }
        val completed = active.copy(
            endAt = now,
            status = SessionStatus.COMPLETED,
            source = source,
        )
        sessionRepository.update(completed)
        // Phase 3+: AllocateSession(sessionId)
        // Phase 4+: evaluate achievements
        // Phase 5+: refresh wallet derive
        timerServiceController.stop()
        return completed
    }
}
