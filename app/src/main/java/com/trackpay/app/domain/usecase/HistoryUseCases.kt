package com.trackpay.app.domain.usecase

import com.trackpay.app.data.repo.JobRepository
import com.trackpay.app.data.repo.SessionRepository
import com.trackpay.app.domain.calc.EarningsCalculator
import com.trackpay.app.domain.model.BreakInput
import com.trackpay.app.domain.model.BreakInterval
import com.trackpay.app.domain.model.HistoryFilter
import com.trackpay.app.domain.model.Job
import com.trackpay.app.domain.model.JobDefaults
import com.trackpay.app.domain.model.SessionDetail
import com.trackpay.app.domain.model.SessionListItem
import com.trackpay.app.domain.model.SessionSource
import com.trackpay.app.domain.model.SessionStatus
import com.trackpay.app.domain.model.SessionTotals
import com.trackpay.app.domain.model.WorkSession
import com.trackpay.app.domain.time.Clock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest

class SessionNotFoundException(id: String) : IllegalArgumentException("Session not found: $id")
class InvalidSessionInputException(message: String) : IllegalArgumentException(message)

/**
 * Pure filter + list-item mapping for history. Safe for JVM unit tests.
 */
fun applyHistoryFilter(
    sessions: List<WorkSession>,
    jobsById: Map<String, Job>,
    filter: HistoryFilter,
    breaksBySessionId: Map<String, List<BreakInterval>> = emptyMap(),
    nowMillis: Long = Long.MAX_VALUE,
): List<SessionListItem> {
    val query = filter.query.trim()
    val queryLower = query.lowercase()

    return sessions
        .asSequence()
        .filter { session ->
            filter.jobId == null || session.jobId == filter.jobId
        }
        .filter { session ->
            val startOk = filter.rangeStartMillis == null || session.startAt >= filter.rangeStartMillis
            val endOk = filter.rangeEndExclusiveMillis == null ||
                session.startAt < filter.rangeEndExclusiveMillis
            startOk && endOk
        }
        .mapNotNull { session ->
            val job = jobsById[session.jobId]
            val jobName = job?.name ?: "Session"
            val jobColor = job?.colorArgb ?: JobDefaults.DEFAULT_COLOR_ARGB
            if (query.isNotEmpty()) {
                val notes = session.notes.orEmpty()
                val matchesName = jobName.lowercase().contains(queryLower)
                val matchesNotes = notes.lowercase().contains(queryLower)
                if (!matchesName && !matchesNotes) return@mapNotNull null
            }
            val breaks = breaksBySessionId[session.id].orEmpty()
            val breakdown = EarningsCalculator.calculate(
                session = session,
                breaks = breaks,
                nowMillis = session.endAt ?: nowMillis,
            )
            SessionListItem(
                session = session,
                jobName = jobName,
                jobColorArgb = jobColor,
                earnedMinor = breakdown.earnedMinor,
                activeMillis = breakdown.activeMillis,
                regularMinutes = breakdown.regularMinutes,
                otMinutes = breakdown.otMinutes,
            )
        }
        .toList()
}

fun computeSessionTotals(items: List<SessionListItem>): SessionTotals =
    SessionTotals(
        earnedMinor = items.sumOf { it.earnedMinor },
        shiftCount = items.size,
        activeMillis = items.sumOf { it.activeMillis },
    )

/**
 * Validates completed-session create/edit inputs.
 * Returns null when valid; otherwise an error message.
 */
fun validateCompletedSessionInput(
    startAt: Long,
    endAt: Long,
    breaks: List<BreakInput>,
): String? {
    if (endAt <= startAt) {
        return "End must be after start"
    }
    for (b in breaks) {
        if (b.endAt <= b.startAt) {
            return "Each break must end after it starts"
        }
        if (b.startAt < startAt || b.endAt > endAt) {
            return "Breaks must fall within the session"
        }
    }
    // Non-overlapping breaks (sorted)
    val ordered = breaks.sortedBy { it.startAt }
    for (i in 1 until ordered.size) {
        if (ordered[i].startAt < ordered[i - 1].endAt) {
            return "Breaks must not overlap"
        }
    }
    return null
}

/**
 * Resolves rate snapshots for edit: keep original unless applyCurrentJobRates.
 */
fun resolveEditSnapshots(
    existing: WorkSession,
    job: Job,
    applyCurrentJobRates: Boolean,
): Triple<Long, Long?, Int?> {
    if (!applyCurrentJobRates) {
        return Triple(
            existing.snapshotHourlyRateMinor,
            existing.snapshotOtRateMinor,
            existing.snapshotOtThresholdMinutes,
        )
    }
    val otRate = job.otRateMinor
    val otThreshold = when {
        otRate == null -> null
        else -> job.otThresholdMinutes ?: JobDefaults.DEFAULT_OT_THRESHOLD_MINUTES
    }
    return Triple(job.hourlyRateMinor, otRate, otThreshold)
}

@Singleton
class ObserveHistoryUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val jobRepository: JobRepository,
    private val clock: Clock,
) {
    /**
     * Emits filtered [SessionListItem]s. Breaks are loaded per emission for accurate earnings.
     * For large histories this is v1-acceptable; Phase 4 may optimize.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(filter: HistoryFilter): Flow<List<SessionListItem>> =
        combine(
            sessionRepository.observeCompletedSessions(),
            jobRepository.observeActiveJobs(),
        ) { sessions, jobs ->
            sessions to jobs.associateBy { it.id }
        }.mapLatest { (sessions, activeJobs) ->
            // Include archived jobs referenced by sessions via getById for names/colors.
            val jobsById = activeJobs.toMutableMap()
            for (session in sessions) {
                if (session.jobId !in jobsById) {
                    jobRepository.getById(session.jobId)?.let { jobsById[it.id] = it }
                }
            }
            val breaksBySessionId = sessions.associate { session ->
                session.id to sessionRepository.listBreaks(session.id)
            }
            applyHistoryFilter(
                sessions = sessions,
                jobsById = jobsById,
                filter = filter,
                breaksBySessionId = breaksBySessionId,
                nowMillis = clock.now(),
            )
        }
}

@Singleton
class GetSessionDetailUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(id: String): SessionDetail? =
        sessionRepository.getSessionDetail(id)
}

@Singleton
class CreateCompletedSessionUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val jobRepository: JobRepository,
    private val hooks: SessionMutationHooks,
) {
    suspend operator fun invoke(
        jobId: String,
        startAt: Long,
        endAt: Long,
        breaks: List<BreakInput> = emptyList(),
        notes: String? = null,
    ): WorkSession {
        validateCompletedSessionInput(startAt, endAt, breaks)?.let {
            throw InvalidSessionInputException(it)
        }
        if (sessionRepository.getActiveSession() != null) {
            throw ActiveSessionExistsException()
        }
        val job = jobRepository.getById(jobId) ?: throw JobNotFoundException(jobId)
        if (job.archived) throw JobNotFoundException(jobId)

        val sessionId = UUID.randomUUID().toString()
        val otRate = job.otRateMinor
        val session = WorkSession(
            id = sessionId,
            jobId = job.id,
            startAt = startAt,
            endAt = endAt,
            status = SessionStatus.COMPLETED,
            snapshotHourlyRateMinor = job.hourlyRateMinor,
            snapshotOtRateMinor = otRate,
            snapshotOtThresholdMinutes = when {
                otRate == null -> null
                else -> job.otThresholdMinutes ?: JobDefaults.DEFAULT_OT_THRESHOLD_MINUTES
            },
            notes = notes?.trim()?.takeIf { it.isNotEmpty() },
            source = SessionSource.MANUAL,
        )
        sessionRepository.insert(session)
        if (breaks.isNotEmpty()) {
            sessionRepository.replaceBreaks(
                sessionId,
                breaks.map { input ->
                    BreakInterval(
                        id = UUID.randomUUID().toString(),
                        sessionId = sessionId,
                        startAt = input.startAt,
                        endAt = input.endAt,
                    )
                },
            )
        }
        hooks.onSessionMutated(sessionId)
        return session
    }
}

@Singleton
class UpdateSessionUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val jobRepository: JobRepository,
    private val hooks: SessionMutationHooks,
) {
    suspend operator fun invoke(
        id: String,
        jobId: String,
        startAt: Long,
        endAt: Long,
        breaks: List<BreakInput> = emptyList(),
        notes: String? = null,
        applyCurrentJobRates: Boolean = false,
    ): WorkSession {
        validateCompletedSessionInput(startAt, endAt, breaks)?.let {
            throw InvalidSessionInputException(it)
        }
        val existing = sessionRepository.getById(id) ?: throw SessionNotFoundException(id)
        if (existing.status != SessionStatus.COMPLETED) {
            throw InvalidSessionStateException("Only completed sessions can be edited")
        }
        val job = jobRepository.getById(jobId) ?: throw JobNotFoundException(jobId)

        val (hourly, otRate, otThreshold) = resolveEditSnapshots(
            existing = existing,
            job = job,
            applyCurrentJobRates = applyCurrentJobRates,
        )

        val updated = existing.copy(
            jobId = job.id,
            startAt = startAt,
            endAt = endAt,
            status = SessionStatus.COMPLETED,
            snapshotHourlyRateMinor = hourly,
            snapshotOtRateMinor = otRate,
            snapshotOtThresholdMinutes = otThreshold,
            notes = notes?.trim()?.takeIf { it.isNotEmpty() },
            source = SessionSource.EDIT,
        )
        sessionRepository.update(updated)
        sessionRepository.replaceBreaks(
            id,
            breaks.map { input ->
                BreakInterval(
                    id = UUID.randomUUID().toString(),
                    sessionId = id,
                    startAt = input.startAt,
                    endAt = input.endAt,
                )
            },
        )
        hooks.onSessionMutated(id)
        return updated
    }
}

@Singleton
class DeleteSessionUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val hooks: SessionMutationHooks,
) {
    suspend operator fun invoke(id: String) {
        val existing = sessionRepository.getById(id) ?: throw SessionNotFoundException(id)
        if (existing.status == SessionStatus.RUNNING || existing.status == SessionStatus.PAUSED) {
            throw InvalidSessionStateException("Cannot delete an active session")
        }
        sessionRepository.deleteSession(id)
        hooks.onSessionDeleted(id)
    }
}

@Singleton
class ComputeSessionTotalsUseCase @Inject constructor() {
    operator fun invoke(items: List<SessionListItem>): SessionTotals =
        computeSessionTotals(items)
}
