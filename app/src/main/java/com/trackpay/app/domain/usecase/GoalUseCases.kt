package com.trackpay.app.domain.usecase

import com.trackpay.app.data.repo.GoalRepository
import com.trackpay.app.data.repo.SessionRepository
import com.trackpay.app.domain.calc.GoalMath
import com.trackpay.app.domain.model.Goal
import com.trackpay.app.domain.model.GoalAllocation
import com.trackpay.app.domain.model.GoalDefaults
import com.trackpay.app.domain.model.GoalProgress
import com.trackpay.app.domain.model.GoalStatus
import com.trackpay.app.domain.model.GoalTemplate
import com.trackpay.app.domain.model.SessionStatus
import com.trackpay.app.domain.time.Clock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow


@Singleton
class UpsertGoalUseCase @Inject constructor(
    private val goalRepository: GoalRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(
        id: String? = null,
        name: String,
        targetMinor: Long,
        deadlineEpochDay: Long,
        iconKey: String = GoalDefaults.DEFAULT_ICON_KEY,
        colorArgb: Int = GoalDefaults.DEFAULT_COLOR_ARGB,
        allocationBps: Int = GoalDefaults.DEFAULT_ALLOCATION_BPS,
        sortOrder: Int? = null,
        status: GoalStatus? = null,
    ): Goal {
        require(name.isNotBlank()) { "Goal name is required" }
        require(targetMinor > 0L) { "Target must be positive" }

        val existing = id?.let { goalRepository.getById(it) }
        val resolvedStatus = status ?: existing?.status ?: GoalStatus.ACTIVE
        val resolvedBps = allocationBps.coerceIn(0, GoalDefaults.BPS_DENOMINATOR)

        if (resolvedStatus == GoalStatus.ACTIVE) {
            val otherActiveBps = goalRepository.sumActiveBpsExcluding(existing?.id)
            GoalMath.validateActiveBpsSum(otherActiveBps, resolvedBps)
        } else {
            require(resolvedBps in 0..GoalDefaults.BPS_DENOMINATOR) {
                "allocationBps must be between 0 and ${GoalDefaults.BPS_DENOMINATOR}"
            }
        }

        val goal = Goal(
            id = existing?.id ?: id ?: UUID.randomUUID().toString(),
            name = name.trim(),
            targetMinor = targetMinor,
            deadlineEpochDay = deadlineEpochDay,
            iconKey = iconKey,
            colorArgb = colorArgb,
            allocationBps = resolvedBps,
            status = resolvedStatus,
            createdAt = existing?.createdAt ?: clock.now(),
            sortOrder = sortOrder ?: existing?.sortOrder,
        )
        goalRepository.upsert(goal)
        return goal
    }
}

@Singleton
class ArchiveGoalUseCase @Inject constructor(
    private val goalRepository: GoalRepository,
) {
    suspend operator fun invoke(goalId: String) {
        goalRepository.archive(goalId)
    }
}

@Singleton
class CompleteGoalUseCase @Inject constructor(
    private val goalRepository: GoalRepository,
) {
    suspend operator fun invoke(goalId: String) {
        goalRepository.markCompleted(goalId)
    }
}

@Singleton
class GetGoalUseCase @Inject constructor(
    private val goalRepository: GoalRepository,
) {
    suspend operator fun invoke(goalId: String): Goal? = goalRepository.getById(goalId)
}

@Singleton
class ListGoalsUseCase @Inject constructor(
    private val goalRepository: GoalRepository,
) {
    operator fun invoke(): Flow<List<Goal>> = goalRepository.observeActiveGoals()

    suspend fun once(): List<Goal> = goalRepository.listActiveGoals()

    fun all(): Flow<List<Goal>> = goalRepository.observeAllGoals()

    suspend fun allOnce(): List<Goal> = goalRepository.listAllGoals()
}

@Singleton
class ObserveGoalProgressUseCase @Inject constructor(
    private val goalRepository: GoalRepository,
) {
    /** Active goals with live saved/pace progress. */
    operator fun invoke(): Flow<List<GoalProgress>> = goalRepository.observeActiveProgress()

    fun observeOne(goalId: String): Flow<GoalProgress?> = goalRepository.observeProgress(goalId)
}

@Singleton
class ListGoalProgressUseCase @Inject constructor(
    private val goalRepository: GoalRepository,
) {
    suspend operator fun invoke(): List<GoalProgress> = goalRepository.listActiveProgress()
}

@Singleton
class ListGoalTemplatesUseCase @Inject constructor() {
    operator fun invoke(): List<GoalTemplate> = GoalDefaults.TEMPLATES
}

@Singleton
class MaterializeGoalTemplateUseCase @Inject constructor(
    private val upsertGoal: UpsertGoalUseCase,
    private val clock: Clock,
) {
    /**
     * Creates an ACTIVE goal from a template (not auto-inserted until user confirms via this call).
     */
    suspend operator fun invoke(
        template: GoalTemplate,
        allocationBps: Int = GoalDefaults.DEFAULT_ALLOCATION_BPS,
        deadlineEpochDay: Long? = null,
    ): Goal {
        val deadline = deadlineEpochDay
            ?: GoalMath.deadlineEpochDayFromMonths(clock.now(), template.defaultHorizonMonths)
        return upsertGoal(
            name = template.name,
            targetMinor = template.defaultTargetMinor,
            deadlineEpochDay = deadline,
            iconKey = template.iconKey,
            colorArgb = template.colorArgb,
            allocationBps = allocationBps,
        )
    }
}

@Singleton
class AllocateSessionUseCase @Inject constructor(
    private val goalRepository: GoalRepository,
    private val sessionRepository: SessionRepository,
    private val clock: Clock,
) {
    /**
     * Recomputes goal allocations for [sessionId].
     * Deletes prior rows; no-ops when session missing or not COMPLETED.
     * Remainder cents after floor splits stay unallocated.
     */
    suspend operator fun invoke(sessionId: String): List<GoalAllocation> {
        goalRepository.deleteAllocationsForSession(sessionId)

        val detail = sessionRepository.getSessionDetail(sessionId) ?: return emptyList()
        if (detail.session.status != SessionStatus.COMPLETED) return emptyList()

        val earned = detail.breakdown.earnedMinor
        val activeGoals = goalRepository.listActiveGoals()
        val splits = GoalMath.allocateAmounts(earned, activeGoals)
        if (splits.isEmpty()) return emptyList()

        val now = clock.now()
        val allocations = splits.map { (goal, amount) ->
            GoalAllocation(
                id = UUID.randomUUID().toString(),
                goalId = goal.id,
                sessionId = sessionId,
                amountMinor = amount,
                createdAt = now,
            )
        }
        goalRepository.insertAllocations(allocations)

        for ((goal, _) in splits) {
            goalRepository.autoCompleteIfReached(goal.id)
        }
        return allocations
    }
}

@Singleton
class RecomputeSessionAllocationsUseCase @Inject constructor(
    private val allocateSession: AllocateSessionUseCase,
) {
    suspend operator fun invoke(sessionId: String): List<GoalAllocation> = allocateSession(sessionId)
}

@Singleton
class RemoveSessionAllocationsUseCase @Inject constructor(
    private val goalRepository: GoalRepository,
) {
    suspend operator fun invoke(sessionId: String) {
        goalRepository.deleteAllocationsForSession(sessionId)
    }
}
