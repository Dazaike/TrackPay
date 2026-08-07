package com.trackpay.app.domain.calc

import com.trackpay.app.domain.model.BreakInterval
import com.trackpay.app.domain.model.InsightsRange
import com.trackpay.app.domain.model.SessionSource
import com.trackpay.app.domain.model.SessionStatus
import com.trackpay.app.domain.model.WorkSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class InsightsMathTest {

    private val zone: ZoneId = ZoneId.of("UTC")

    /** Monday 2024-01-08 09:00 UTC */
    private val mondayEpochDay: Long = LocalDate.of(2024, 1, 8).toEpochDay()

    private fun millisAt(epochDay: Long, hour: Int = 9, minute: Int = 0): Long =
        LocalDate.ofEpochDay(epochDay)
            .atTime(hour, minute)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()

    private fun session(
        id: String,
        startDay: Long,
        hours: Long = 1L,
        hourly: Long = 2_500L,
        otThresholdMinutes: Int? = null,
        otRate: Long? = null,
        startHour: Int = 9,
        activeMinutesOverride: Long? = null,
    ): InsightsMath.SessionEarnings {
        val start = millisAt(startDay, startHour)
        val end = if (activeMinutesOverride != null) {
            start + activeMinutesOverride * 60_000L
        } else {
            start + hours * 3_600_000L
        }
        val work = WorkSession(
            id = id,
            jobId = "job",
            startAt = start,
            endAt = end,
            status = SessionStatus.COMPLETED,
            snapshotHourlyRateMinor = hourly,
            snapshotOtRateMinor = otRate,
            snapshotOtThresholdMinutes = otThresholdMinutes,
            notes = null,
            source = SessionSource.MANUAL,
        )
        val breakdown = EarningsCalculator.calculate(work, emptyList(), end)
        return InsightsMath.SessionEarnings(work, breakdown)
    }

    @Test
    fun challenge_target_equals_previous_iso_week_sum() {
        // Previous ISO week: Mon 2024-01-01 .. Sun 2024-01-07
        val prevMon = LocalDate.of(2024, 1, 1).toEpochDay()
        val sessions = listOf(
            session("a", prevMon, hours = 2, hourly = 1_000L), // 2000
            session("b", prevMon + 2, hours = 1, hourly = 1_000L), // 1000
            session("c", mondayEpochDay, hours = 3, hourly = 1_000L), // this week
        )
        val challenge = InsightsMath.weeklyChallenge(
            sessions = sessions,
            todayEpochDay = mondayEpochDay + 2, // Wed
            zoneId = zone,
            primaryHourlyMinor = 5_000L,
        )
        assertEquals(mondayEpochDay, challenge.weekStartEpochDay)
        assertEquals(3_000L, challenge.targetMinor)
        assertEquals(3_000L, challenge.earnedMinor)
        assertEquals(5, challenge.daysLeft) // Wed → Sun inclusive remaining days: Wed,Thu,Fri,Sat,Sun = 5
    }

    @Test
    fun challenge_target_falls_back_to_floor_or_hourly() {
        val emptyPrev = InsightsMath.challengeTargetMinor(
            previousWeekEarnedMinor = 0L,
            primaryHourlyMinor = null,
        )
        assertEquals(10_000L, emptyPrev)

        val fromRate = InsightsMath.challengeTargetMinor(
            previousWeekEarnedMinor = 0L,
            primaryHourlyMinor = 1_000L, // *20 = 20000
        )
        assertEquals(20_000L, fromRate)

        val floorWins = InsightsMath.challengeTargetMinor(
            previousWeekEarnedMinor = 0L,
            primaryHourlyMinor = 100L, // *20 = 2000 < floor
        )
        assertEquals(10_000L, floorWins)

        val usesPrev = InsightsMath.challengeTargetMinor(
            previousWeekEarnedMinor = 4_200L,
            primaryHourlyMinor = 9_999L,
        )
        assertEquals(4_200L, usesPrev)
    }

    @Test
    fun range_totals_and_day_buckets_for_7d() {
        val today = mondayEpochDay + 3 // Thu
        val sessions = listOf(
            session("a", today - 1, hours = 2, hourly = 1_000L), // 2000
            session("b", today, hours = 1, hourly = 1_000L), // 1000
            session("old", today - 10, hours = 5, hourly = 1_000L), // outside 7D
        )
        val summary = InsightsMath.rangeSummary(
            sessions = sessions,
            range = InsightsRange.D7,
            todayEpochDay = today,
            zoneId = zone,
        )
        assertEquals(3_000L, summary.earnedMinor)
        assertEquals(7, summary.buckets.size)
        assertEquals(2_000L, summary.buckets[5].earnedMinor) // today-1 is 6th of 7 (index 5)
        assertEquals(1_000L, summary.buckets[6].earnedMinor)
    }

    @Test
    fun range_90d_uses_week_buckets() {
        val today = mondayEpochDay + 6 // Sun
        val sessions = listOf(
            session("a", mondayEpochDay, hours = 1, hourly = 1_000L),
            session("b", mondayEpochDay + 8, hours = 1, hourly = 1_000L), // next week — outside if today is sun of first week... wait
        )
        // Use today far enough to include both
        val today2 = mondayEpochDay + 10
        val summary = InsightsMath.rangeSummary(
            sessions = sessions,
            range = InsightsRange.D90,
            todayEpochDay = today2,
            zoneId = zone,
        )
        assertEquals(2_000L, summary.earnedMinor)
        assertTrue(summary.buckets.size >= 2)
        assertTrue(summary.buckets.any { it.earnedMinor == 1_000L })
    }

    @Test
    fun weekday_averages_and_highlight_index() {
        val today = mondayEpochDay + 6 // Sunday
        val sessions = listOf(
            session("m1", mondayEpochDay, hours = 1, hourly = 1_000L), // Mon 1000
            session("m2", mondayEpochDay + 7, hours = 3, hourly = 1_000L), // next Mon 3000 — outside if range ends today?
            session("w1", mondayEpochDay + 2, hours = 4, hourly = 1_000L), // Wed 4000
            session("f1", mondayEpochDay + 4, hours = 2, hourly = 1_000L), // Fri 2000
        )
        // Extend today so both Mondays fit in 14d window via D30
        val today2 = mondayEpochDay + 7
        val avgs = InsightsMath.weekdayAverages(
            sessions = sessions,
            range = InsightsRange.D30,
            todayEpochDay = today2,
            zoneId = zone,
        )
        assertEquals(7, avgs.size)
        assertEquals(1, avgs[0].dayOfWeek) // Mon
        // Mon samples: 1000 and 3000 → mean 2000
        assertEquals(2, avgs[0].sampleCount)
        assertEquals(2_000L, avgs[0].earnedMinor)
        assertEquals(4_000L, avgs[2].earnedMinor) // Wed
        val highlight = InsightsMath.highlightWeekdayIndex(avgs)
        assertEquals(2, highlight) // Wednesday index in Mon..Sun
    }

    @Test
    fun streak_allows_yesterday_when_today_empty() {
        val today = mondayEpochDay + 3 // Thu
        val sessions = listOf(
            session("a", today - 2, hours = 1), // Tue
            session("b", today - 1, hours = 1), // Wed
        )
        val streak = InsightsMath.streakState(sessions, today, zone)
        assertEquals(2, streak.currentDays)
        assertEquals(2, streak.bestDays)
        assertEquals(today - 1, streak.lastActiveLocalDateEpochDay)
    }

    @Test
    fun streak_includes_today_and_breaks_on_gap() {
        val today = mondayEpochDay + 4 // Fri
        val sessions = listOf(
            session("a", today - 4, hours = 1), // Mon
            session("b", today - 3, hours = 1), // Tue
            // Wed missing
            session("c", today - 1, hours = 1), // Thu
            session("d", today, hours = 1), // Fri
        )
        val streak = InsightsMath.streakState(sessions, today, zone)
        assertEquals(2, streak.currentDays)
        assertEquals(2, streak.bestDays)
    }

    @Test
    fun streak_midnight_edge_with_fixed_zone() {
        // Session just after local midnight counts as that local day
        val day = mondayEpochDay
        val start = millisAt(day, hour = 0, minute = 5)
        val end = start + 3_600_000L
        val work = WorkSession(
            id = "mid",
            jobId = "job",
            startAt = start,
            endAt = end,
            status = SessionStatus.COMPLETED,
            snapshotHourlyRateMinor = 1_000L,
            snapshotOtRateMinor = null,
            snapshotOtThresholdMinutes = null,
            notes = null,
            source = SessionSource.MANUAL,
        )
        val item = InsightsMath.SessionEarnings(
            work,
            EarningsCalculator.calculate(work, emptyList(), end),
        )
        val streak = InsightsMath.streakState(listOf(item), day, zone)
        assertEquals(1, streak.currentDays)
        assertEquals(day, streak.lastActiveLocalDateEpochDay)

        // Next calendar day with no work: streak still 1 via yesterday rule
        val next = InsightsMath.streakState(listOf(item), day + 1, zone)
        assertEquals(1, next.currentDays)

        // Two days later: broken
        val broken = InsightsMath.streakState(listOf(item), day + 2, zone)
        assertEquals(0, broken.currentDays)
        assertEquals(1, broken.bestDays)
    }

    @Test
    fun zero_active_minutes_does_not_count_for_streak() {
        val today = mondayEpochDay
        val start = millisAt(today)
        val work = WorkSession(
            id = "z",
            jobId = "job",
            startAt = start,
            endAt = start + 60_000L,
            status = SessionStatus.COMPLETED,
            snapshotHourlyRateMinor = 1_000L,
            snapshotOtRateMinor = null,
            snapshotOtThresholdMinutes = null,
            notes = null,
            source = SessionSource.MANUAL,
        )
        // Entire session is a break → 0 active minutes
        val breaks = listOf(
            BreakInterval(
                id = "b",
                sessionId = "z",
                startAt = start,
                endAt = start + 60_000L,
            ),
        )
        val item = InsightsMath.SessionEarnings(
            work,
            EarningsCalculator.calculate(work, breaks, start + 60_000L),
        )
        assertEquals(0L, item.breakdown.activeMinutes)
        val streak = InsightsMath.streakState(listOf(item), today, zone)
        assertEquals(0, streak.currentDays)
    }

    @Test
    fun lifetime_earned_sums_completed_only() {
        val sessions = listOf(
            session("a", mondayEpochDay, hours = 1, hourly = 1_000L),
            session("b", mondayEpochDay + 1, hours = 2, hourly = 1_000L),
        )
        assertEquals(3_000L, InsightsMath.lifetimeEarnedMinor(sessions))
    }

    @Test
    fun week_warrior_detects_five_active_days_in_iso_week() {
        val mon = mondayEpochDay
        val sessions = (0 until 5).map { offset ->
            session("d$offset", mon + offset, hours = 1)
        }
        assertTrue(InsightsMath.hasWeekWithActiveDays(sessions, minDays = 5, zoneId = zone))
        assertFalse(
            InsightsMath.hasWeekWithActiveDays(sessions.take(4), minDays = 5, zoneId = zone),
        )
    }
}
