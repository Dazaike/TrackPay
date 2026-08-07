package com.trackpay.app.domain.calc

import com.trackpay.app.domain.model.BreakInterval
import com.trackpay.app.domain.model.EarningsBreakdown
import com.trackpay.app.domain.model.JobDefaults
import com.trackpay.app.domain.model.WorkSession
import kotlin.math.max
import kotlin.math.min

/**
 * Pure pay math. Earnings are always derived from wall-clock timestamps +
 * rate snapshots + breaks — never from a stored running total.
 *
 * OT v1: threshold applies to **this session's** active minutes only
 * (multi-session same-day merge deferred).
 */
object EarningsCalculator {

    fun calculate(
        session: WorkSession,
        breaks: List<BreakInterval>,
        nowMillis: Long,
    ): EarningsBreakdown =
        calculate(
            startAt = session.startAt,
            endAt = session.endAt,
            breaks = breaks,
            snapshotHourlyRateMinor = session.snapshotHourlyRateMinor,
            snapshotOtRateMinor = session.snapshotOtRateMinor,
            snapshotOtThresholdMinutes = session.snapshotOtThresholdMinutes,
            nowMillis = nowMillis,
        )

    fun calculate(
        startAt: Long,
        endAt: Long?,
        breaks: List<BreakInterval>,
        snapshotHourlyRateMinor: Long,
        snapshotOtRateMinor: Long?,
        snapshotOtThresholdMinutes: Int?,
        nowMillis: Long,
    ): EarningsBreakdown {
        val rangeEnd = endAt ?: nowMillis
        val activeMillis = activeMillis(
            startAt = startAt,
            rangeEnd = rangeEnd,
            breaks = breaks,
            openBreakEnd = nowMillis,
        )
        val activeMinutes = activeMillis / 60_000L

        val (regularMinutes, otMinutes) = splitRegularOt(
            activeMinutes = activeMinutes,
            otRateMinor = snapshotOtRateMinor,
            otThresholdMinutes = snapshotOtThresholdMinutes,
        )

        val earnedMinor = earnedFromMillis(
            activeMillis = activeMillis,
            hourlyRateMinor = snapshotHourlyRateMinor,
            otRateMinor = snapshotOtRateMinor,
            otThresholdMinutes = snapshotOtThresholdMinutes,
        )

        return EarningsBreakdown(
            activeMillis = activeMillis,
            activeMinutes = activeMinutes,
            regularMinutes = regularMinutes,
            otMinutes = otMinutes,
            earnedMinor = earnedMinor,
        )
    }

    fun activeMillis(
        startAt: Long,
        rangeEnd: Long,
        breaks: List<BreakInterval>,
        openBreakEnd: Long,
    ): Long {
        if (rangeEnd <= startAt) return 0L
        var breakMillis = 0L
        for (b in breaks) {
            val bStart = max(b.startAt, startAt)
            val rawEnd = b.endAt ?: openBreakEnd
            val bEnd = min(rawEnd, rangeEnd)
            if (bEnd > bStart) {
                breakMillis += bEnd - bStart
            }
        }
        return max(0L, rangeEnd - startAt - breakMillis)
    }

    private fun splitRegularOt(
        activeMinutes: Long,
        otRateMinor: Long?,
        otThresholdMinutes: Int?,
    ): Pair<Long, Long> {
        if (otRateMinor == null) {
            return activeMinutes to 0L
        }
        val threshold = (otThresholdMinutes ?: JobDefaults.DEFAULT_OT_THRESHOLD_MINUTES).toLong()
        val regular = min(activeMinutes, threshold)
        val ot = max(0L, activeMinutes - threshold)
        return regular to ot
    }

    /**
     * Sub-minute precision for live `$` ticks using millisecond proportions of hourly rates.
     */
    private fun earnedFromMillis(
        activeMillis: Long,
        hourlyRateMinor: Long,
        otRateMinor: Long?,
        otThresholdMinutes: Int?,
    ): Long {
        if (activeMillis <= 0L) return 0L
        if (otRateMinor == null) {
            return (activeMillis * hourlyRateMinor) / 3_600_000L
        }
        val thresholdMillis =
            (otThresholdMinutes ?: JobDefaults.DEFAULT_OT_THRESHOLD_MINUTES).toLong() * 60_000L
        val regularMillis = min(activeMillis, thresholdMillis)
        val otMillis = max(0L, activeMillis - thresholdMillis)
        val regularEarned = (regularMillis * hourlyRateMinor) / 3_600_000L
        val otEarned = (otMillis * otRateMinor) / 3_600_000L
        return regularEarned + otEarned
    }
}
