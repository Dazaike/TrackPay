package com.trackpay.app.domain.calc

import com.trackpay.app.domain.model.BreakInterval
import com.trackpay.app.domain.model.SessionSource
import com.trackpay.app.domain.model.SessionStatus
import com.trackpay.app.domain.model.WorkSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EarningsCalculatorTest {

    private val start = 1_700_000_000_000L

    private fun session(
        hourly: Long = 2_500L, // $25.00/hr
        ot: Long? = null,
        otThresholdMinutes: Int? = null,
        endAt: Long? = null,
        status: SessionStatus = SessionStatus.COMPLETED,
    ) = WorkSession(
        id = "s1",
        jobId = "j1",
        startAt = start,
        endAt = endAt,
        status = status,
        snapshotHourlyRateMinor = hourly,
        snapshotOtRateMinor = ot,
        snapshotOtThresholdMinutes = otThresholdMinutes,
        notes = null,
        source = SessionSource.MANUAL,
    )

    @Test
    fun earnings_noBreaks_oneHour() {
        val end = start + 3_600_000L
        val result = EarningsCalculator.calculate(
            session = session(endAt = end),
            breaks = emptyList(),
            nowMillis = end,
        )
        assertEquals(60L, result.activeMinutes)
        assertEquals(60L, result.regularMinutes)
        assertEquals(0L, result.otMinutes)
        assertEquals(2_500L, result.earnedMinor)
    }

    @Test
    fun pause_excluded_from_active_time() {
        // 2h wall, 30m pause → 90m active → $37.50 at $25/hr
        val end = start + 7_200_000L
        val breaks = listOf(
            BreakInterval(
                id = "b1",
                sessionId = "s1",
                startAt = start + 3_600_000L,
                endAt = start + 3_600_000L + 1_800_000L,
            ),
        )
        val result = EarningsCalculator.calculate(
            session = session(endAt = end),
            breaks = breaks,
            nowMillis = end,
        )
        assertEquals(90L, result.activeMinutes)
        assertEquals(3_750L, result.earnedMinor)
    }

    @Test
    fun open_break_uses_now_as_end() {
        val now = start + 3_600_000L // 1h wall with open break from t+30m
        val breaks = listOf(
            BreakInterval(
                id = "b1",
                sessionId = "s1",
                startAt = start + 1_800_000L,
                endAt = null,
            ),
        )
        val result = EarningsCalculator.calculate(
            session = session(endAt = null, status = SessionStatus.PAUSED),
            breaks = breaks,
            nowMillis = now,
        )
        assertEquals(30L, result.activeMinutes)
        assertEquals(1_250L, result.earnedMinor)
    }

    @Test
    fun ot_above_threshold() {
        // $20/hr regular, $30/hr OT, threshold 60 minutes, 90 minutes active
        val end = start + 5_400_000L
        val result = EarningsCalculator.calculate(
            session = session(
                hourly = 2_000L,
                ot = 3_000L,
                otThresholdMinutes = 60,
                endAt = end,
            ),
            breaks = emptyList(),
            nowMillis = end,
        )
        assertEquals(90L, result.activeMinutes)
        assertEquals(60L, result.regularMinutes)
        assertEquals(30L, result.otMinutes)
        // 60m * $20 + 30m * $30 = $20 + $15 = $35
        assertEquals(3_500L, result.earnedMinor)
    }

    @Test
    fun uses_snapshot_rates_not_current_job_rates() {
        val end = start + 3_600_000L
        val snapshotted = session(hourly = 1_000L, endAt = end) // $10/hr snapshot
        val result = EarningsCalculator.calculate(
            session = snapshotted,
            breaks = emptyList(),
            nowMillis = end,
        )
        // If live job were $50/hr, calculator must still use $10 snapshot
        assertEquals(1_000L, result.earnedMinor)
        assertTrue(result.earnedMinor != 5_000L)
    }

    @Test
    fun sub_minute_live_tick_is_monotonic_proportion() {
        val thirtyMin = start + 1_800_000L
        val result = EarningsCalculator.calculate(
            session = session(hourly = 3_600L, endAt = null, status = SessionStatus.RUNNING),
            breaks = emptyList(),
            nowMillis = thirtyMin,
        )
        // $36/hr * 0.5h = $18.00
        assertEquals(1_800L, result.earnedMinor)
        assertEquals(30L, result.activeMinutes)
    }

    @Test
    fun zero_when_now_before_start() {
        val result = EarningsCalculator.calculate(
            session = session(endAt = null, status = SessionStatus.RUNNING),
            breaks = emptyList(),
            nowMillis = start - 1_000L,
        )
        assertEquals(0L, result.earnedMinor)
        assertEquals(0L, result.activeMillis)
    }
}
