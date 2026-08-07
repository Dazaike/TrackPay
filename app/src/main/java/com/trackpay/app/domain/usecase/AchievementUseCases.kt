package com.trackpay.app.domain.usecase

import com.trackpay.app.data.repo.AchievementRepository
import com.trackpay.app.data.repo.InsightsRepository
import com.trackpay.app.domain.calc.InsightsMath
import com.trackpay.app.domain.model.AchievementStatus
import com.trackpay.app.domain.time.Clock
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ObserveAchievementsUseCase @Inject constructor(
    private val achievementRepository: AchievementRepository,
) {
    operator fun invoke(): Flow<List<AchievementStatus>> =
        achievementRepository.observeStatuses()
}

/**
 * Evaluates the fixed achievement catalog against current sessions/goals and
 * persists new unlocks idempotently. Returns newly unlocked ids.
 */
@Singleton
class EvaluateAchievementsUseCase @Inject constructor(
    private val insightsRepository: InsightsRepository,
    private val achievementRepository: AchievementRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(): List<String> {
        val input = insightsRepository.achievementEvalInput()
        val eligible = InsightsMath.evaluateAchievementIds(input)
        if (eligible.isEmpty()) return emptyList()
        return achievementRepository.unlockAll(eligible, clock.now())
    }
}

/**
 * Session-complete side effect: re-evaluate achievements after allocations.
 */
@Singleton
class AchievementSessionHooks @Inject constructor(
    private val evaluateAchievements: EvaluateAchievementsUseCase,
) : SessionMutationHooks {
    override suspend fun onSessionCompleted(sessionId: String) {
        evaluateAchievements()
    }

    override suspend fun onSessionMutated(sessionId: String) {
        evaluateAchievements()
    }

    // Deletes do not revoke unlocks.
}
