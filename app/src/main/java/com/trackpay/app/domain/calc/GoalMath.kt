package com.trackpay.app.domain.calc

import com.trackpay.app.domain.model.Goal
import com.trackpay.app.domain.model.GoalDefaults
import com.trackpay.app.domain.model.GoalProgress
import com.trackpay.app.domain.model.GoalStatus
import java.time.Instant
import java.time.ZoneId
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

class InvalidGoalException(message: String) : IllegalArgumentException(message)

/**
 * Pure goal allocation + pace helpers (unit-tested without Room).
 *
 * Allocation uses floor(earned * bps / 10000); remainder cents stay unallocated.
 */
object GoalMath {
    fun allocateAmounts(
        earnedMinor: Long,
        goals: List<Goal>,
    ): List<Pair<Goal, Long>> {
        if (earnedMinor <= 0L) return emptyList()
        return goals
            .filter { it.status == GoalStatus.ACTIVE && it.allocationBps > 0 }
            .map { goal ->
                val amount = floorBps(earnedMinor, goal.allocationBps)
                goal to amount
            }
            .filter { it.second > 0L }
    }

    fun floorBps(earnedMinor: Long, bps: Int): Long {
        if (earnedMinor <= 0L || bps <= 0) return 0L
        return earnedMinor * bps.toLong() / GoalDefaults.BPS_DENOMINATOR.toLong()
    }

    fun validateActiveBpsSum(existingActiveBpsExcludingSelf: Int, newBps: Int) {
        require(newBps in 0..GoalDefaults.BPS_DENOMINATOR) {
            "allocationBps must be between 0 and ${GoalDefaults.BPS_DENOMINATOR}"
        }
        val total = existingActiveBpsExcludingSelf + newBps
        if (total > GoalDefaults.BPS_DENOMINATOR) {
            throw InvalidGoalException(
                "Active goals allocation exceeds 100% (total bps=$total)",
            )
        }
    }

    fun epochDayLocal(nowMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): Long =
        Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate().toEpochDay()

    fun deadlineEpochDayFromMonths(
        nowMillis: Long,
        months: Int,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Long {
        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        return today.plusMonths(months.toLong()).toEpochDay()
    }

    /**
     * Fractional weeks until deadline. Overdue → [overdue]=true and weeksLeft uses 1.0
     * so pace still yields a weekly catch-up figure. Never below 1/7 week.
     */
    fun weeksLeft(
        deadlineEpochDay: Long,
        todayEpochDay: Long,
    ): Pair<Double, Boolean> {
        val days = deadlineEpochDay - todayEpochDay
        if (days < 0L) {
            return 1.0 to true
        }
        val weeks = max(days.toDouble() / 7.0, 1.0 / 7.0)
        return weeks to false
    }

    fun pacePerWeekMinor(remainingMinor: Long, weeksLeft: Double): Long {
        if (remainingMinor <= 0L) return 0L
        if (weeksLeft <= 0.0) return remainingMinor
        return ceil(remainingMinor.toDouble() / weeksLeft).toLong()
    }

    fun progressRatio(savedMinor: Long, targetMinor: Long): Float {
        if (targetMinor <= 0L) return 0f
        return min(1f, savedMinor.toFloat() / targetMinor.toFloat())
    }

    fun remainingMinor(targetMinor: Long, savedMinor: Long): Long =
        max(0L, targetMinor - savedMinor)

    fun shouldAutoComplete(goal: Goal, savedMinor: Long): Boolean =
        goal.status == GoalStatus.ACTIVE &&
            goal.targetMinor > 0L &&
            savedMinor >= goal.targetMinor

    fun buildProgress(
        goal: Goal,
        savedMinor: Long,
        todayEpochDay: Long,
    ): GoalProgress {
        val remaining = remainingMinor(goal.targetMinor, savedMinor)
        val (weeks, overdue) = weeksLeft(goal.deadlineEpochDay, todayEpochDay)
        return GoalProgress(
            goal = goal,
            savedMinor = savedMinor,
            remainingMinor = remaining,
            progress = progressRatio(savedMinor, goal.targetMinor),
            pacePerWeekMinor = pacePerWeekMinor(remaining, weeks),
            overdue = overdue,
        )
    }
}
