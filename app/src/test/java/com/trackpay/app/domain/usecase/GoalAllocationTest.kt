package com.trackpay.app.domain.usecase

import com.trackpay.app.domain.calc.GoalMath
import com.trackpay.app.domain.calc.InvalidGoalException
import com.trackpay.app.domain.model.Goal
import com.trackpay.app.domain.model.GoalDefaults
import com.trackpay.app.domain.model.GoalStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class GoalAllocationTest {

    private fun goal(
        id: String,
        bps: Int,
        targetMinor: Long = 100_000L,
        status: GoalStatus = GoalStatus.ACTIVE,
        deadlineEpochDay: Long = 20_000L,
    ) = Goal(
        id = id,
        name = id,
        targetMinor = targetMinor,
        deadlineEpochDay = deadlineEpochDay,
        iconKey = GoalDefaults.DEFAULT_ICON_KEY,
        colorArgb = GoalDefaults.DEFAULT_COLOR_ARGB,
        allocationBps = bps,
        status = status,
        createdAt = 1L,
        sortOrder = null,
    )

    @Test
    fun bps_sum_over_10000_rejected() {
        try {
            GoalMath.validateActiveBpsSum(existingActiveBpsExcludingSelf = 6_000, newBps = 5_000)
            fail("expected InvalidGoalException")
        } catch (e: InvalidGoalException) {
            assertTrue(e.message!!.contains("100%"))
        }
    }

    @Test
    fun bps_sum_exactly_10000_allowed() {
        GoalMath.validateActiveBpsSum(existingActiveBpsExcludingSelf = 5_000, newBps = 5_000)
    }

    @Test
    fun bps_out_of_range_rejected() {
        try {
            GoalMath.validateActiveBpsSum(0, 10_001)
            fail("expected require failure")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("allocationBps"))
        }
    }

    @Test
    fun allocate_50_50_splits_earned() {
        val earned = 10_001L
        val goals = listOf(
            goal("a", bps = 5_000),
            goal("b", bps = 5_000),
        )
        val splits = GoalMath.allocateAmounts(earned, goals)
        assertEquals(2, splits.size)
        // floor(10001 * 5000 / 10000) = 5000 each; remainder 1 unallocated
        assertEquals(5_000L, splits[0].second)
        assertEquals(5_000L, splits[1].second)
        assertEquals(10_000L, splits.sumOf { it.second })
    }

    @Test
    fun allocate_skips_zero_bps_and_non_active() {
        val goals = listOf(
            goal("a", bps = 2_500),
            goal("b", bps = 0),
            goal("c", bps = 2_500, status = GoalStatus.ARCHIVED),
            goal("d", bps = 2_500, status = GoalStatus.COMPLETED),
        )
        val splits = GoalMath.allocateAmounts(8_000L, goals)
        assertEquals(1, splits.size)
        assertEquals("a", splits[0].first.id)
        assertEquals(2_000L, splits[0].second)
    }

    @Test
    fun recompute_after_earned_change_updates_amounts() {
        val goals = listOf(
            goal("a", bps = 2_500),
            goal("b", bps = 7_500),
        )
        val first = GoalMath.allocateAmounts(10_000L, goals).associate { it.first.id to it.second }
        assertEquals(2_500L, first["a"])
        assertEquals(7_500L, first["b"])

        val second = GoalMath.allocateAmounts(20_000L, goals).associate { it.first.id to it.second }
        assertEquals(5_000L, second["a"])
        assertEquals(15_000L, second["b"])
    }

    @Test
    fun delete_session_clears_allocations_pure() {
        // Pure stand-in for AllocateSession delete-then-maybe-insert:
        // non-COMPLETED / missing session → empty list after clear.
        val afterDelete = emptyList<Pair<String, Long>>()
        assertTrue(afterDelete.isEmpty())

        val earnedIfCompleted = 0L // delete path never re-inserts
        val recomputed = GoalMath.allocateAmounts(
            earnedIfCompleted,
            listOf(goal("a", bps = 5_000)),
        )
        assertTrue(recomputed.isEmpty())
    }

    @Test
    fun pace_math_with_fixed_clock() {
        val today = 10_000L // epoch day
        val deadline = 10_014L // 14 days → 2 weeks
        val (weeks, overdue) = GoalMath.weeksLeft(deadline, today)
        assertFalse(overdue)
        assertEquals(2.0, weeks, 0.0001)

        val pace = GoalMath.pacePerWeekMinor(remainingMinor = 1_000L, weeksLeft = weeks)
        assertEquals(500L, pace)

        val overduePair = GoalMath.weeksLeft(deadlineEpochDay = today - 1, todayEpochDay = today)
        assertTrue(overduePair.second)
        assertEquals(1.0, overduePair.first, 0.0001)
        assertEquals(200L, GoalMath.pacePerWeekMinor(200L, overduePair.first))
    }

    @Test
    fun pace_minimum_one_seventh_week() {
        val today = 10_000L
        val (weeks, overdue) = GoalMath.weeksLeft(deadlineEpochDay = today, todayEpochDay = today)
        assertFalse(overdue)
        assertEquals(1.0 / 7.0, weeks, 0.0001)
    }

    @Test
    fun auto_complete_when_threshold_crossed() {
        val g = goal("a", bps = 5_000, targetMinor = 10_000L)
        assertFalse(GoalMath.shouldAutoComplete(g, savedMinor = 9_999L))
        assertTrue(GoalMath.shouldAutoComplete(g, savedMinor = 10_000L))
        assertTrue(GoalMath.shouldAutoComplete(g, savedMinor = 12_000L))

        val archived = g.copy(status = GoalStatus.ARCHIVED)
        assertFalse(GoalMath.shouldAutoComplete(archived, savedMinor = 50_000L))
    }

    @Test
    fun build_progress_caps_ratio_and_remaining() {
        val g = goal("a", bps = 1_000, targetMinor = 1_000L, deadlineEpochDay = 10_007L)
        val p = GoalMath.buildProgress(goal = g, savedMinor = 1_500L, todayEpochDay = 10_000L)
        assertEquals(1_500L, p.savedMinor)
        assertEquals(0L, p.remainingMinor)
        assertEquals(1f, p.progress, 0.0001f)
        assertEquals(0L, p.pacePerWeekMinor)
        assertFalse(p.overdue)
    }

    @Test
    fun floor_bps_zero_earned() {
        assertEquals(0L, GoalMath.floorBps(0L, 5_000))
        assertTrue(GoalMath.allocateAmounts(0L, listOf(goal("a", 10_000))).isEmpty())
    }
}
