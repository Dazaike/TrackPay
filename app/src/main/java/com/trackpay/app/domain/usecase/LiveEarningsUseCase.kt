package com.trackpay.app.domain.usecase

import com.trackpay.app.domain.calc.EarningsCalculator
import com.trackpay.app.domain.model.ActiveSession
import com.trackpay.app.domain.model.EarningsBreakdown
import com.trackpay.app.domain.model.WorkSession
import com.trackpay.app.domain.model.BreakInterval
import com.trackpay.app.domain.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LiveEarningsUseCase @Inject constructor(
    private val clock: Clock,
) {
    operator fun invoke(active: ActiveSession): EarningsBreakdown =
        EarningsCalculator.calculate(
            session = active.session,
            breaks = active.breaks,
            nowMillis = clock.now(),
        )

    operator fun invoke(
        session: WorkSession,
        breaks: List<BreakInterval>,
        nowMillis: Long = clock.now(),
    ): EarningsBreakdown =
        EarningsCalculator.calculate(
            session = session,
            breaks = breaks,
            nowMillis = nowMillis,
        )
}
