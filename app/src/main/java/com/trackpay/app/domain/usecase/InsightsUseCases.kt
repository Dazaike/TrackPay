package com.trackpay.app.domain.usecase

import com.trackpay.app.data.repo.InsightsRepository
import com.trackpay.app.domain.model.InsightsRange
import com.trackpay.app.domain.model.InsightsRangeSummary
import com.trackpay.app.domain.model.InsightsSnapshot
import com.trackpay.app.domain.model.StreakState
import com.trackpay.app.domain.model.WeekdayAverage
import com.trackpay.app.domain.model.WeeklyChallenge
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ObserveWeeklyChallengeUseCase @Inject constructor(
    private val insightsRepository: InsightsRepository,
) {
    operator fun invoke(): Flow<WeeklyChallenge> = insightsRepository.observeWeeklyChallenge()
}

@Singleton
class ObserveRangeSummaryUseCase @Inject constructor(
    private val insightsRepository: InsightsRepository,
) {
    operator fun invoke(range: InsightsRange): Flow<InsightsRangeSummary> =
        insightsRepository.observeRangeSummary(range)
}

@Singleton
class ObserveWeekdayAveragesUseCase @Inject constructor(
    private val insightsRepository: InsightsRepository,
) {
    operator fun invoke(range: InsightsRange): Flow<List<WeekdayAverage>> =
        insightsRepository.observeWeekdayAverages(range)
}

@Singleton
class ObserveStreakUseCase @Inject constructor(
    private val insightsRepository: InsightsRepository,
) {
    operator fun invoke(): Flow<StreakState> = insightsRepository.observeStreak()
}

@Singleton
class ObserveLifetimeEarnedUseCase @Inject constructor(
    private val insightsRepository: InsightsRepository,
) {
    operator fun invoke(): Flow<Long> = insightsRepository.observeLifetimeEarned()
}

@Singleton
class ObserveInsightsUseCase @Inject constructor(
    private val insightsRepository: InsightsRepository,
) {
    operator fun invoke(range: InsightsRange = InsightsRange.D7): Flow<InsightsSnapshot> =
        insightsRepository.observeSnapshot(range)
}
