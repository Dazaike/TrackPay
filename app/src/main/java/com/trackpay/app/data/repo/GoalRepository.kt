package com.trackpay.app.data.repo

import com.trackpay.app.data.local.dao.GoalAllocationDao
import com.trackpay.app.data.local.dao.GoalDao
import com.trackpay.app.data.local.toDomain
import com.trackpay.app.data.local.toEntity
import com.trackpay.app.domain.model.Goal
import com.trackpay.app.domain.model.GoalAllocation
import com.trackpay.app.domain.model.GoalProgress
import com.trackpay.app.domain.model.GoalStatus
import com.trackpay.app.domain.time.Clock
import com.trackpay.app.domain.calc.GoalMath
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoalRepository @Inject constructor(
    private val goalDao: GoalDao,
    private val goalAllocationDao: GoalAllocationDao,
    private val clock: Clock,
) {
    fun observeActiveGoals(): Flow<List<Goal>> =
        goalDao.observeActive().map { rows -> rows.map { it.toDomain() } }

    fun observeAllGoals(): Flow<List<Goal>> =
        goalDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    suspend fun listActiveGoals(): List<Goal> =
        goalDao.listActive().map { it.toDomain() }

    suspend fun listAllGoals(): List<Goal> =
        goalDao.listAll().map { it.toDomain() }

    suspend fun getById(id: String): Goal? =
        goalDao.getById(id)?.toDomain()

    fun observeById(id: String): Flow<Goal?> =
        goalDao.observeById(id).map { it?.toDomain() }

    suspend fun upsert(goal: Goal) {
        goalDao.upsert(goal.toEntity())
    }

    suspend fun archive(id: String) {
        goalDao.archive(id)
    }

    suspend fun markCompleted(id: String) {
        goalDao.markCompleted(id)
    }

    suspend fun sumActiveBpsExcluding(excludeId: String?): Int =
        goalDao.sumActiveBpsExcluding(excludeId)

    suspend fun insertAllocations(allocations: List<GoalAllocation>) {
        if (allocations.isEmpty()) return
        goalAllocationDao.insertAll(allocations.map { it.toEntity() })
    }

    suspend fun deleteAllocationsForSession(sessionId: String) {
        goalAllocationDao.deleteForSession(sessionId)
    }

    suspend fun deleteAllocationsForGoal(goalId: String) {
        goalAllocationDao.deleteForGoal(goalId)
    }

    suspend fun listAllocationsForSession(sessionId: String): List<GoalAllocation> =
        goalAllocationDao.listForSession(sessionId).map { it.toDomain() }

    suspend fun listAllocationsForGoal(goalId: String): List<GoalAllocation> =
        goalAllocationDao.listForGoal(goalId).map { it.toDomain() }

    suspend fun sumAllocated(goalId: String): Long =
        goalAllocationDao.sumForGoal(goalId)

    fun observeActiveProgress(): Flow<List<GoalProgress>> =
        combine(
            goalDao.observeActive(),
            goalAllocationDao.observeAll(),
        ) { goals, allocations ->
            val sums = allocations
                .groupBy { it.goalId }
                .mapValues { (_, rows) -> rows.sumOf { it.amountMinor } }
            val today = GoalMath.epochDayLocal(clock.now())
            goals.map { entity ->
                val goal = entity.toDomain()
                GoalMath.buildProgress(
                    goal = goal,
                    savedMinor = sums[goal.id] ?: 0L,
                    todayEpochDay = today,
                )
            }
        }.distinctUntilChanged()

    fun observeProgress(goalId: String): Flow<GoalProgress?> =
        combine(
            goalDao.observeById(goalId),
            goalAllocationDao.observeSumForGoal(goalId),
        ) { entity, saved ->
            val goal = entity?.toDomain() ?: return@combine null
            GoalMath.buildProgress(
                goal = goal,
                savedMinor = saved,
                todayEpochDay = GoalMath.epochDayLocal(clock.now()),
            )
        }.distinctUntilChanged()

    suspend fun progressFor(goal: Goal): GoalProgress {
        val saved = sumAllocated(goal.id)
        return GoalMath.buildProgress(
            goal = goal,
            savedMinor = saved,
            todayEpochDay = GoalMath.epochDayLocal(clock.now()),
        )
    }

    suspend fun listActiveProgress(): List<GoalProgress> {
        val today = GoalMath.epochDayLocal(clock.now())
        return listActiveGoals().map { goal ->
            GoalMath.buildProgress(
                goal = goal,
                savedMinor = sumAllocated(goal.id),
                todayEpochDay = today,
            )
        }
    }

    suspend fun autoCompleteIfReached(goalId: String) {
        val goal = getById(goalId) ?: return
        if (goal.status != GoalStatus.ACTIVE) return
        val saved = sumAllocated(goalId)
        if (GoalMath.shouldAutoComplete(goal, saved)) {
            markCompleted(goalId)
        }
    }
}
