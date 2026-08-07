package com.trackpay.app.location

import com.trackpay.app.domain.model.Job
import com.trackpay.app.domain.model.SessionStatus
import com.trackpay.app.domain.model.WorkSession

/**
 * Pure decision logic for geofence ENTER/EXIT → suggest clock in/out.
 * Default product behavior is actionable notification (not silent auto clock).
 */
object GeofenceTransitionHandler {

    enum class Transition {
        ENTER,
        EXIT,
    }

    sealed class Suggestion {
        data class ClockIn(
            val jobId: String,
            val jobName: String,
        ) : Suggestion()

        data class ClockOut(
            val jobId: String,
            val jobName: String,
            val sessionId: String,
        ) : Suggestion()

        data object None : Suggestion()
    }

    fun decide(
        transition: Transition,
        job: Job?,
        activeSession: WorkSession?,
        geoMasterEnabled: Boolean,
    ): Suggestion {
        if (!geoMasterEnabled) return Suggestion.None
        if (job == null || job.archived || !job.geoEnabled) return Suggestion.None
        if (job.latitude == null || job.longitude == null) return Suggestion.None

        return when (transition) {
            Transition.ENTER -> {
                if (activeSession != null) return Suggestion.None
                Suggestion.ClockIn(jobId = job.id, jobName = job.name)
            }
            Transition.EXIT -> {
                val session = activeSession ?: return Suggestion.None
                if (session.jobId != job.id) return Suggestion.None
                if (session.status != SessionStatus.RUNNING && session.status != SessionStatus.PAUSED) {
                    return Suggestion.None
                }
                Suggestion.ClockOut(
                    jobId = job.id,
                    jobName = job.name,
                    sessionId = session.id,
                )
            }
        }
    }
}
