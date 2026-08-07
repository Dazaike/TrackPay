package com.trackpay.app.location

import com.trackpay.app.domain.model.Job
import com.trackpay.app.domain.model.JobDefaults
import com.trackpay.app.domain.model.SessionSource
import com.trackpay.app.domain.model.SessionStatus
import com.trackpay.app.domain.model.WorkSession
import com.trackpay.app.location.GeofenceTransitionHandler.Suggestion
import com.trackpay.app.location.GeofenceTransitionHandler.Transition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeofenceTransitionHandlerTest {

    private val geoJob = Job(
        id = "job-cafe",
        name = "Cafe",
        hourlyRateMinor = 2_500L,
        otRateMinor = null,
        otThresholdMinutes = null,
        colorArgb = JobDefaults.DEFAULT_COLOR_ARGB,
        iconKey = JobDefaults.DEFAULT_ICON_KEY,
        archived = false,
        createdAt = 1L,
        geoEnabled = true,
        latitude = 37.77,
        longitude = -122.42,
        radiusMeters = 150,
    )

    private val otherJob = geoJob.copy(id = "job-other", name = "Warehouse")

    private fun activeSession(
        jobId: String = geoJob.id,
        status: SessionStatus = SessionStatus.RUNNING,
    ) = WorkSession(
        id = "sess-1",
        jobId = jobId,
        startAt = 1_000L,
        endAt = null,
        status = status,
        snapshotHourlyRateMinor = 2_500L,
        snapshotOtRateMinor = null,
        snapshotOtThresholdMinutes = null,
        notes = null,
        source = SessionSource.MANUAL,
    )

    @Test
    fun enter_idle_suggests_clock_in() {
        val result = GeofenceTransitionHandler.decide(
            transition = Transition.ENTER,
            job = geoJob,
            activeSession = null,
            geoMasterEnabled = true,
        )
        assertEquals(Suggestion.ClockIn(geoJob.id, geoJob.name), result)
    }

    @Test
    fun enter_with_active_session_suggests_none() {
        val result = GeofenceTransitionHandler.decide(
            transition = Transition.ENTER,
            job = geoJob,
            activeSession = activeSession(),
            geoMasterEnabled = true,
        )
        assertEquals(Suggestion.None, result)
    }

    @Test
    fun exit_matching_active_suggests_clock_out() {
        val session = activeSession()
        val result = GeofenceTransitionHandler.decide(
            transition = Transition.EXIT,
            job = geoJob,
            activeSession = session,
            geoMasterEnabled = true,
        )
        assertEquals(
            Suggestion.ClockOut(geoJob.id, geoJob.name, session.id),
            result,
        )
    }

    @Test
    fun exit_paused_matching_suggests_clock_out() {
        val session = activeSession(status = SessionStatus.PAUSED)
        val result = GeofenceTransitionHandler.decide(
            transition = Transition.EXIT,
            job = geoJob,
            activeSession = session,
            geoMasterEnabled = true,
        )
        assertTrue(result is Suggestion.ClockOut)
    }

    @Test
    fun exit_different_job_suggests_none() {
        val result = GeofenceTransitionHandler.decide(
            transition = Transition.EXIT,
            job = geoJob,
            activeSession = activeSession(jobId = otherJob.id),
            geoMasterEnabled = true,
        )
        assertEquals(Suggestion.None, result)
    }

    @Test
    fun master_off_blocks_all() {
        val enter = GeofenceTransitionHandler.decide(
            transition = Transition.ENTER,
            job = geoJob,
            activeSession = null,
            geoMasterEnabled = false,
        )
        val exit = GeofenceTransitionHandler.decide(
            transition = Transition.EXIT,
            job = geoJob,
            activeSession = activeSession(),
            geoMasterEnabled = false,
        )
        assertEquals(Suggestion.None, enter)
        assertEquals(Suggestion.None, exit)
    }

    @Test
    fun geo_disabled_job_suggests_none() {
        val job = geoJob.copy(geoEnabled = false)
        val result = GeofenceTransitionHandler.decide(
            transition = Transition.ENTER,
            job = job,
            activeSession = null,
            geoMasterEnabled = true,
        )
        assertEquals(Suggestion.None, result)
    }

    @Test
    fun missing_coordinates_suggests_none() {
        val job = geoJob.copy(latitude = null, longitude = null)
        val result = GeofenceTransitionHandler.decide(
            transition = Transition.ENTER,
            job = job,
            activeSession = null,
            geoMasterEnabled = true,
        )
        assertEquals(Suggestion.None, result)
    }

    @Test
    fun archived_job_suggests_none() {
        val job = geoJob.copy(archived = true)
        val result = GeofenceTransitionHandler.decide(
            transition = Transition.ENTER,
            job = job,
            activeSession = null,
            geoMasterEnabled = true,
        )
        assertEquals(Suggestion.None, result)
    }

    @Test
    fun null_job_suggests_none() {
        val result = GeofenceTransitionHandler.decide(
            transition = Transition.ENTER,
            job = null,
            activeSession = null,
            geoMasterEnabled = true,
        )
        assertEquals(Suggestion.None, result)
    }

    /**
     * Fake use-case wiring: notification action invokes clock in/out with GEOFENCE source.
     */
    @Test
    fun action_invokes_use_cases_with_geofence_source() {
        val clockIns = mutableListOf<Pair<String, SessionSource>>()
        val clockOuts = mutableListOf<SessionSource>()

        fun onSuggestion(suggestion: Suggestion) {
            when (suggestion) {
                is Suggestion.ClockIn -> clockIns += suggestion.jobId to SessionSource.GEOFENCE
                is Suggestion.ClockOut -> clockOuts += SessionSource.GEOFENCE
                Suggestion.None -> Unit
            }
        }

        onSuggestion(
            GeofenceTransitionHandler.decide(
                Transition.ENTER, geoJob, null, true,
            ),
        )
        onSuggestion(
            GeofenceTransitionHandler.decide(
                Transition.EXIT, geoJob, activeSession(), true,
            ),
        )

        assertEquals(listOf(geoJob.id to SessionSource.GEOFENCE), clockIns)
        assertEquals(listOf(SessionSource.GEOFENCE), clockOuts)
    }
}
