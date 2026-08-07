package com.trackpay.app.data.repo

import com.trackpay.app.data.local.dao.BreakIntervalDao
import com.trackpay.app.data.local.dao.GoalAllocationDao
import com.trackpay.app.data.local.dao.GoalDao
import com.trackpay.app.data.local.dao.JobDao
import com.trackpay.app.data.local.dao.WorkSessionDao
import com.trackpay.app.data.local.toDomain
import com.trackpay.app.domain.calc.InsightsMath
import com.trackpay.app.domain.calc.InsightsMath.SessionEarnings
import com.trackpay.app.domain.model.BreakInterval
import com.trackpay.app.domain.model.InsightsRange
import com.trackpay.app.domain.model.InsightsRangeSummary
import com.trackpay.app.domain.model.InsightsSnapshot
import com.trackpay.app.domain.model.StreakState
import com.trackpay.app.domain.model.WeekdayAverage
import com.trackpay.app.domain.model.WeeklyChallenge
import com.trackpay.app.domain.model.WorkSession
import com.trackpay.app.domain.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InsightsRepository @Inject constructor(
    private val workSessionDao: WorkSessionDao,
    private val breakIntervalDao: BreakIntervalDao,
    private val jobDao: JobDao,
    private val goalDao: GoalDao,
    private val goalAllocationDao: GoalAllocationDao,
    private val achievementRepository: AchievementRepository,
    private val clock: Clock,
) {
    private fun zone(): ZoneId = ZoneId.systemDefault()

    suspend fun loadSessionEarnings(): List<SessionEarnings> {
        val sessions = workSessionDao.listAllCompleted().map { it.toDomain() }
        return attachEarnings(sessions)
    }

    fun observeSessionEarnings(): Flow<List<SessionEarnings>> =
        combine(
            workSessionDao.observeCompleted(),
            breakIntervalDao.observeAll(),
        ) { sessionRows, breakRows ->
            val sessions = sessionRows.map { it.toDomain() }
            val breaksBySession = breakRows
                .map { it.toDomain() }
                .groupBy { it.sessionId }
            val now = clock.now()
            sessions.map { session ->
                InsightsMath.toSessionEarnings(
                    session = session,
                    breaks = breaksBySession[session.id].orEmpty(),
                    nowMillis = now,
                )
            }
        }.distinctUntilChanged()

    private suspend fun attachEarnings(sessions: List<WorkSession>): List<SessionEarnings> {
        if (sessions.isEmpty()) return emptyList()
        val ids = sessions.map { it.id }
        val breaks = if (ids.size == 1) {
            breakIntervalDao.listForSession(ids.first()).map { it.toDomain() }
        } else {
            // Room IN () with empty is avoided above; large lists still OK for local DB.
            breakIntervalDao.listForSessions(ids).map { it.toDomain() }
        }
        val bySession = breaks.groupBy { it.sessionId }
        val now = clock.now()
        return sessions.map { session ->
            InsightsMath.toSessionEarnings(
                session = session,
                breaks = bySession[session.id].orEmpty(),
                nowMillis = now,
            )
        }
    }

    suspend fun primaryHourlyMinor(): Long? =
        jobDao.listActiveJobs().minByOrNull { it.createdAt }?.hourlyRateMinor

    fun observePrimaryHourlyMinor(): Flow<Long?> =
        jobDao.observeActiveJobs().map { jobs ->
            jobs.minByOrNull { it.createdAt }?.hourlyRateMinor
        }

    suspend fun hasAnyGoal(): Boolean = goalDao.listAll().isNotEmpty()

    fun observeHasAnyGoal(): Flow<Boolean> =
        goalDao.observeAll().map { it.isNotEmpty() }

    suspend fun hasPositiveAllocation(): Boolean =
        goalAllocationDao.listAll().any { it.amountMinor > 0L }

    fun observeHasPositiveAllocation(): Flow<Boolean> =
        goalAllocationDao.observeAll().map { rows -> rows.any { it.amountMinor > 0L } }

    fun observeWeeklyChallenge(): Flow<WeeklyChallenge> =
        combine(observeSessionEarnings(), observePrimaryHourlyMinor()) { sessions, hourly ->
            val today = InsightsMath.epochDay(clock.now(), zone())
            InsightsMath.weeklyChallenge(sessions, today, zone(), hourly)
        }.distinctUntilChanged()

    fun observeRangeSummary(range: InsightsRange): Flow<InsightsRangeSummary> =
        observeSessionEarnings().map { sessions ->
            val today = InsightsMath.epochDay(clock.now(), zone())
            InsightsMath.rangeSummary(sessions, range, today, zone())
        }.distinctUntilChanged()

    fun observeWeekdayAverages(range: InsightsRange): Flow<List<WeekdayAverage>> =
        observeSessionEarnings().map { sessions ->
            val today = InsightsMath.epochDay(clock.now(), zone())
            InsightsMath.weekdayAverages(sessions, range, today, zone())
        }.distinctUntilChanged()

    fun observeStreak(): Flow<StreakState> =
        observeSessionEarnings().map { sessions ->
            val today = InsightsMath.epochDay(clock.now(), zone())
            InsightsMath.streakState(sessions, today, zone())
        }.distinctUntilChanged()

    fun observeLifetimeEarned(): Flow<Long> =
        observeSessionEarnings().map { InsightsMath.lifetimeEarnedMinor(it) }
            .distinctUntilChanged()

    suspend fun lifetimeEarnedMinor(): Long =
        InsightsMath.lifetimeEarnedMinor(loadSessionEarnings())

    fun observeSnapshot(range: InsightsRange): Flow<InsightsSnapshot> =
        combine(
            observeSessionEarnings(),
            observePrimaryHourlyMinor(),
            achievementRepository.observeStatuses(),
            observeHasAnyGoal(),
            observeHasPositiveAllocation(),
        ) { sessions, hourly, achievements, _, _ ->
            val today = InsightsMath.epochDay(clock.now(), zone())
            val z = zone()
            InsightsSnapshot(
                weeklyChallenge = InsightsMath.weeklyChallenge(sessions, today, z, hourly),
                rangeSummary = InsightsMath.rangeSummary(sessions, range, today, z),
                weekdayAverages = InsightsMath.weekdayAverages(sessions, range, today, z),
                streak = InsightsMath.streakState(sessions, today, z),
                achievements = achievements,
                lifetimeEarnedMinor = InsightsMath.lifetimeEarnedMinor(sessions),
                hasCompletedSessions = sessions.any {
                    it.session.status == com.trackpay.app.domain.model.SessionStatus.COMPLETED
                },
            )
        }.distinctUntilChanged()

    suspend fun achievementEvalInput(): InsightsMath.AchievementEvalInput {
        val sessions = loadSessionEarnings()
        val today = InsightsMath.epochDay(clock.now(), zone())
        val streak = InsightsMath.streakState(sessions, today, zone())
        return InsightsMath.AchievementEvalInput(
            sessions = sessions,
            hasAnyGoal = hasAnyGoal(),
            hasPositiveAllocation = hasPositiveAllocation(),
            streakCurrentDays = streak.currentDays,
            lifetimeEarnedMinor = InsightsMath.lifetimeEarnedMinor(sessions),
            zoneId = zone(),
        )
    }
}
