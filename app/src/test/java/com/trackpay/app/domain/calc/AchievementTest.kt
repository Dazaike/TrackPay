package com.trackpay.app.domain.calc

import com.trackpay.app.domain.model.AchievementIds
import com.trackpay.app.domain.model.SessionSource
import com.trackpay.app.domain.model.SessionStatus
import com.trackpay.app.domain.model.WorkSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class AchievementTest {

    private val zone: ZoneId = ZoneId.of("UTC")
    private val monday: Long = LocalDate.of(2024, 1, 8).toEpochDay()

    private fun millisAt(epochDay: Long, hour: Int = 9): Long =
        LocalDate.ofEpochDay(epochDay)
            .atTime(hour, 0)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()

    private fun completed(
        id: String,
        day: Long,
        hour: Int = 9,
        hours: Long = 1L,
        hourly: Long = 2_500L,
        otThreshold: Int? = null,
        otRate: Long? = null,
    ): InsightsMath.SessionEarnings {
        val start = millisAt(day, hour)
        val end = start + hours * 3_600_000L
        val work = WorkSession(
            id = id,
            jobId = "job",
            startAt = start,
            endAt = end,
            status = SessionStatus.COMPLETED,
            snapshotHourlyRateMinor = hourly,
            snapshotOtRateMinor = otRate,
            snapshotOtThresholdMinutes = otThreshold,
            notes = null,
            source = SessionSource.MANUAL,
        )
        return InsightsMath.SessionEarnings(
            work,
            EarningsCalculator.calculate(work, emptyList(), end),
        )
    }

    private fun eval(
        sessions: List<InsightsMath.SessionEarnings>,
        hasGoal: Boolean = false,
        hasAlloc: Boolean = false,
        streak: Int = 0,
        lifetime: Long? = null,
    ): Set<String> {
        val life = lifetime ?: InsightsMath.lifetimeEarnedMinor(sessions)
        return InsightsMath.evaluateAchievementIds(
            InsightsMath.AchievementEvalInput(
                sessions = sessions,
                hasAnyGoal = hasGoal,
                hasPositiveAllocation = hasAlloc,
                streakCurrentDays = streak,
                lifetimeEarnedMinor = life,
                zoneId = zone,
            ),
        )
    }

    @Test
    fun empty_state_unlocks_nothing() {
        assertTrue(eval(emptyList()).isEmpty())
    }

    @Test
    fun first_shift_on_any_completed() {
        val ids = eval(listOf(completed("a", monday)))
        assertTrue(AchievementIds.FIRST_SHIFT in ids)
    }

    @Test
    fun early_bird_before_7_local() {
        val early = eval(listOf(completed("a", monday, hour = 6)))
        assertTrue(AchievementIds.EARLY_BIRD in early)

        val late = eval(listOf(completed("b", monday, hour = 8)))
        assertFalse(AchievementIds.EARLY_BIRD in late)
    }

    @Test
    fun week_warrior_five_days() {
        val five = (0 until 5).map { completed("d$it", monday + it) }
        assertTrue(AchievementIds.WEEK_WARRIOR in eval(five))
        assertFalse(AchievementIds.WEEK_WARRIOR in eval(five.take(4)))
    }

    @Test
    fun ot_hero_when_ot_minutes_positive() {
        // 2h session, threshold 60 → 60 OT minutes
        val withOt = completed(
            id = "ot",
            day = monday,
            hours = 2L,
            hourly = 1_000L,
            otThreshold = 60,
            otRate = 1_500L,
        )
        assertTrue(withOt.breakdown.otMinutes > 0L)
        assertTrue(AchievementIds.OT_HERO in eval(listOf(withOt)))

        val noOt = completed("n", monday, hours = 1L, hourly = 1_000L, otThreshold = 60, otRate = 1_500L)
        assertEquals(0L, noOt.breakdown.otMinutes)
        assertFalse(AchievementIds.OT_HERO in eval(listOf(noOt)))
    }

    @Test
    fun goal_flags() {
        val base = listOf(completed("a", monday))
        assertTrue(AchievementIds.GOAL_STARTER in eval(base, hasGoal = true))
        assertTrue(AchievementIds.GOAL_FUNDED in eval(base, hasAlloc = true))
        assertFalse(AchievementIds.GOAL_STARTER in eval(base, hasGoal = false))
        assertFalse(AchievementIds.GOAL_FUNDED in eval(base, hasAlloc = false))
    }

    @Test
    fun streak_7_from_current_or_best() {
        val days = (0 until 7).map { completed("s$it", monday + it) }
        val today = monday + 6
        val streak = InsightsMath.streakState(days, today, zone)
        assertEquals(7, streak.currentDays)
        assertTrue(AchievementIds.STREAK_7 in eval(days, streak = streak.currentDays))

        // Broken current but best still ≥7
        val later = monday + 20
        val cold = InsightsMath.streakState(days, later, zone)
        assertEquals(0, cold.currentDays)
        assertEquals(7, cold.bestDays)
        assertTrue(AchievementIds.STREAK_7 in eval(days, streak = cold.currentDays))
    }

    @Test
    fun earned_1k_at_100000_minors() {
        // 40h * $25 = $1000 = 100000 minor
        val big = completed("big", monday, hours = 40L, hourly = 2_500L)
        assertEquals(100_000L, big.breakdown.earnedMinor)
        assertTrue(AchievementIds.EARNED_1K in eval(listOf(big)))

        val small = completed("s", monday, hours = 1L, hourly = 2_500L)
        assertFalse(AchievementIds.EARNED_1K in eval(listOf(small)))
    }

    @Test
    fun evaluate_is_deterministic_set_idempotent_for_store() {
        val sessions = listOf(
            completed("a", monday, hour = 6),
            completed("b", monday + 1),
        )
        val first = eval(sessions, hasGoal = true, hasAlloc = true)
        val second = eval(sessions, hasGoal = true, hasAlloc = true)
        assertEquals(first, second)
        assertTrue(first.containsAll(
            listOf(
                AchievementIds.FIRST_SHIFT,
                AchievementIds.EARLY_BIRD,
                AchievementIds.GOAL_STARTER,
                AchievementIds.GOAL_FUNDED,
            ),
        ))
    }

    @Test
    fun catalog_ids_match_phase_brief() {
        val expected = setOf(
            "first_shift",
            "early_bird",
            "week_warrior",
            "ot_hero",
            "goal_starter",
            "goal_funded",
            "streak_7",
            "earned_1k",
        )
        assertEquals(
            expected,
            com.trackpay.app.domain.model.AchievementCatalog.ALL.map { it.id }.toSet(),
        )
    }
}
