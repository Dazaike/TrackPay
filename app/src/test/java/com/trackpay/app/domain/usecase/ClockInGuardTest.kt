package com.trackpay.app.domain.usecase

import com.trackpay.app.domain.model.Job
import com.trackpay.app.domain.model.JobDefaults
import com.trackpay.app.domain.model.SessionSource
import com.trackpay.app.domain.model.SessionStatus
import com.trackpay.app.domain.model.WorkSession
import com.trackpay.app.domain.time.Clock
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure guard logic for single active session — mirrors ClockInUseCase without Android/Hilt.
 */
class ClockInGuardTest {

    private class FakeClock(private val t: Long = 1_000L) : Clock {
        override fun now(): Long = t
    }

    private class SessionStore {
        var active: WorkSession? = null
        var inserted: WorkSession? = null

        suspend fun getActiveSession(): WorkSession? = active

        suspend fun insert(session: WorkSession) {
            inserted = session
            active = session
        }
    }

    private suspend fun clockIn(
        sessions: SessionStore,
        job: Job?,
        jobId: String,
        clock: Clock,
    ): WorkSession {
        if (sessions.getActiveSession() != null) throw ActiveSessionExistsException()
        val found = job?.takeIf { it.id == jobId && !it.archived }
            ?: throw JobNotFoundException(jobId)
        val session = WorkSession(
            id = "new",
            jobId = found.id,
            startAt = clock.now(),
            endAt = null,
            status = SessionStatus.RUNNING,
            snapshotHourlyRateMinor = found.hourlyRateMinor,
            snapshotOtRateMinor = found.otRateMinor,
            snapshotOtThresholdMinutes = found.otThresholdMinutes,
            notes = null,
            source = SessionSource.MANUAL,
        )
        sessions.insert(session)
        return session
    }

    private val sampleJob = Job(
        id = "job-1",
        name = "Cafe",
        hourlyRateMinor = 2_500L,
        otRateMinor = null,
        otThresholdMinutes = null,
        colorArgb = JobDefaults.DEFAULT_COLOR_ARGB,
        iconKey = JobDefaults.DEFAULT_ICON_KEY,
        archived = false,
        createdAt = 1L,
    )

    @Test
    fun clockIn_rejected_when_active_session_exists() = runBlocking {
        val sessions = SessionStore().apply {
            active = WorkSession(
                id = "existing",
                jobId = "job-1",
                startAt = 500L,
                endAt = null,
                status = SessionStatus.RUNNING,
                snapshotHourlyRateMinor = 2_500L,
                snapshotOtRateMinor = null,
                snapshotOtThresholdMinutes = null,
                notes = null,
                source = SessionSource.MANUAL,
            )
        }
        try {
            clockIn(sessions, sampleJob, "job-1", FakeClock())
            throw AssertionError("Expected ActiveSessionExistsException")
        } catch (_: ActiveSessionExistsException) {
            // expected
        }
        assertEquals(null, sessions.inserted)
    }

    @Test
    fun clockIn_snapshots_rates_and_starts_running() = runBlocking {
        val sessions = SessionStore()
        val created = clockIn(sessions, sampleJob, "job-1", FakeClock(42L))
        assertEquals(SessionStatus.RUNNING, created.status)
        assertEquals(2_500L, created.snapshotHourlyRateMinor)
        assertEquals(42L, created.startAt)
        assertEquals("job-1", created.jobId)
        assertTrue(sessions.inserted != null)
    }
}
