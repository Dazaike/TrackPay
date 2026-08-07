package com.trackpay.app.domain.calc

import com.trackpay.app.domain.model.BreakInterval
import com.trackpay.app.domain.model.EarningsBreakdown
import com.trackpay.app.domain.model.InsightsRange
import com.trackpay.app.domain.model.InsightsRangeSummary
import com.trackpay.app.domain.model.RangeBucket
import com.trackpay.app.domain.model.SessionStatus
import com.trackpay.app.domain.model.StreakState
import com.trackpay.app.domain.model.WeekdayAverage
import com.trackpay.app.domain.model.WeeklyChallenge
import com.trackpay.app.domain.model.WorkSession
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.max

/**
 * Pure insights aggregations over completed sessions (unit-tested, no Room).
 *
 * Weeks are **ISO Monday-start**. Ranges are rolling local-TZ windows ending today.
 */
object InsightsMath {

    /** $100 floor in minor units when previous week earned nothing. */
    const val CHALLENGE_FLOOR_MINOR: Long = 10_000L

    /** Hours used for primary-job fallback target (hourly * hours). */
    const val CHALLENGE_FALLBACK_HOURS: Long = 20L


    private val dayLabelFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d", Locale.US)
    private val monthDayFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.US)

    data class SessionEarnings(
        val session: WorkSession,
        val breakdown: EarningsBreakdown,
    )

    fun localDate(millis: Long, zoneId: ZoneId): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zoneId).toLocalDate()

    fun epochDay(millis: Long, zoneId: ZoneId): Long =
        localDate(millis, zoneId).toEpochDay()

    fun startOfDayMillis(epochDay: Long, zoneId: ZoneId): Long =
        LocalDate.ofEpochDay(epochDay).atStartOfDay(zoneId).toInstant().toEpochMilli()

    /** Inclusive start epoch-day of the ISO week containing [epochDay]. */
    fun isoWeekStartEpochDay(epochDay: Long): Long {
        val date = LocalDate.ofEpochDay(epochDay)
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toEpochDay()
    }

    fun isoDayOfWeek(epochDay: Long): Int =
        LocalDate.ofEpochDay(epochDay).dayOfWeek.value // 1=Mon … 7=Sun

    fun daysLeftInIsoWeek(todayEpochDay: Long): Int {
        val start = isoWeekStartEpochDay(todayEpochDay)
        val endExclusive = start + 7
        return (endExclusive - todayEpochDay).toInt().coerceAtLeast(0)
    }

    /**
     * Challenge target = sum of completed earnings in the previous ISO week.
     * If that sum is 0 → max([CHALLENGE_FLOOR_MINOR], primaryHourlyMinor * 20).
     */
    fun challengeTargetMinor(
        previousWeekEarnedMinor: Long,
        primaryHourlyMinor: Long?,
    ): Long {
        if (previousWeekEarnedMinor > 0L) return previousWeekEarnedMinor
        val fromRate = (primaryHourlyMinor ?: 0L) * CHALLENGE_FALLBACK_HOURS
        return max(CHALLENGE_FLOOR_MINOR, fromRate)
    }

    fun weeklyChallenge(
        sessions: List<SessionEarnings>,
        todayEpochDay: Long,
        zoneId: ZoneId,
        primaryHourlyMinor: Long?,
    ): WeeklyChallenge {
        val weekStart = isoWeekStartEpochDay(todayEpochDay)
        val prevStart = weekStart - 7
        val thisWeek = sumEarnedInEpochDayRange(sessions, weekStart, weekStart + 7, zoneId)
        val prevWeek = sumEarnedInEpochDayRange(sessions, prevStart, weekStart, zoneId)
        return WeeklyChallenge(
            weekStartEpochDay = weekStart,
            targetMinor = challengeTargetMinor(prevWeek, primaryHourlyMinor),
            earnedMinor = thisWeek,
            daysLeft = daysLeftInIsoWeek(todayEpochDay),
        )
    }

    fun sumEarnedInEpochDayRange(
        sessions: List<SessionEarnings>,
        startEpochDayInclusive: Long,
        endEpochDayExclusive: Long,
        zoneId: ZoneId,
    ): Long {
        var sum = 0L
        for (item in sessions) {
            if (item.session.status != SessionStatus.COMPLETED) continue
            val day = epochDay(item.session.startAt, zoneId)
            if (day in startEpochDayInclusive until endEpochDayExclusive) {
                sum += item.breakdown.earnedMinor
            }
        }
        return sum
    }

    fun lifetimeEarnedMinor(sessions: List<SessionEarnings>): Long {
        var sum = 0L
        for (item in sessions) {
            if (item.session.status != SessionStatus.COMPLETED) continue
            sum += item.breakdown.earnedMinor
        }
        return sum
    }

    /**
     * Rolling range ending today (inclusive). Buckets:
     * - 7D / 30D → one bar per local day
     * - 90D / 1Y → one bar per ISO week (Monday start)
     */
    fun rangeSummary(
        sessions: List<SessionEarnings>,
        range: InsightsRange,
        todayEpochDay: Long,
        zoneId: ZoneId,
    ): InsightsRangeSummary {
        val startDay = todayEpochDay - range.dayCount + 1
        val endExclusive = todayEpochDay + 1
        val filtered = sessions.filter { item ->
            item.session.status == SessionStatus.COMPLETED &&
                epochDay(item.session.startAt, zoneId) in startDay until endExclusive
        }
        var totalEarned = 0L
        var totalMillis = 0L
        for (item in filtered) {
            totalEarned += item.breakdown.earnedMinor
            totalMillis += item.breakdown.activeMillis
        }
        val buckets = when (range) {
            InsightsRange.D7, InsightsRange.D30 ->
                dayBuckets(filtered, startDay, endExclusive, zoneId)
            InsightsRange.D90, InsightsRange.Y1 ->
                weekBuckets(filtered, startDay, endExclusive, zoneId)
        }
        return InsightsRangeSummary(
            earnedMinor = totalEarned,
            activeMillis = totalMillis,
            buckets = buckets,
        )
    }

    private fun dayBuckets(
        sessions: List<SessionEarnings>,
        startDay: Long,
        endExclusive: Long,
        zoneId: ZoneId,
    ): List<RangeBucket> {
        val byDay = LinkedHashMap<Long, MutableList<SessionEarnings>>()
        var d = startDay
        while (d < endExclusive) {
            byDay[d] = mutableListOf()
            d++
        }
        for (item in sessions) {
            val day = epochDay(item.session.startAt, zoneId)
            byDay[day]?.add(item)
        }
        return byDay.map { (day, items) ->
            var earned = 0L
            var millis = 0L
            for (item in items) {
                earned += item.breakdown.earnedMinor
                millis += item.breakdown.activeMillis
            }
            val date = LocalDate.ofEpochDay(day)
            RangeBucket(
                startEpochDay = day,
                label = date.format(dayLabelFmt),
                earnedMinor = earned,
                activeMillis = millis,
            )
        }
    }

    private fun weekBuckets(
        sessions: List<SessionEarnings>,
        startDay: Long,
        endExclusive: Long,
        zoneId: ZoneId,
    ): List<RangeBucket> {
        val firstWeekStart = isoWeekStartEpochDay(startDay)
        val lastWeekStart = isoWeekStartEpochDay(endExclusive - 1)
        val byWeek = LinkedHashMap<Long, MutableList<SessionEarnings>>()
        var w = firstWeekStart
        while (w <= lastWeekStart) {
            byWeek[w] = mutableListOf()
            w += 7
        }
        for (item in sessions) {
            val day = epochDay(item.session.startAt, zoneId)
            if (day !in startDay until endExclusive) continue
            val week = isoWeekStartEpochDay(day)
            byWeek[week]?.add(item)
        }
        return byWeek.map { (weekStart, items) ->
            var earned = 0L
            var millis = 0L
            for (item in items) {
                earned += item.breakdown.earnedMinor
                millis += item.breakdown.activeMillis
            }
            val date = LocalDate.ofEpochDay(weekStart)
            RangeBucket(
                startEpochDay = weekStart,
                label = date.format(monthDayFmt),
                earnedMinor = earned,
                activeMillis = millis,
            )
        }
    }

    /**
     * Mean earnings/hours per ISO weekday over the selected range.
     * Always returns 7 entries Mon…Sun (sampleCount may be 0).
     */
    fun weekdayAverages(
        sessions: List<SessionEarnings>,
        range: InsightsRange,
        todayEpochDay: Long,
        zoneId: ZoneId,
    ): List<WeekdayAverage> {
        val startDay = todayEpochDay - range.dayCount + 1
        val endExclusive = todayEpochDay + 1
        val totalsEarned = LongArray(8)
        val totalsMillis = LongArray(8)
        val counts = IntArray(8)
        for (item in sessions) {
            if (item.session.status != SessionStatus.COMPLETED) continue
            val day = epochDay(item.session.startAt, zoneId)
            if (day !in startDay until endExclusive) continue
            val dow = isoDayOfWeek(day)
            totalsEarned[dow] += item.breakdown.earnedMinor
            totalsMillis[dow] += item.breakdown.activeMillis
            counts[dow] += 1
        }
        return (1..7).map { dow ->
            val n = counts[dow]
            WeekdayAverage(
                dayOfWeek = dow,
                earnedMinor = if (n == 0) 0L else totalsEarned[dow] / n,
                activeMillis = if (n == 0) 0L else totalsMillis[dow] / n,
                sampleCount = n,
            )
        }
    }

    /** Index of max average earnings in Mon…Sun list; -1 if all zero / empty. */
    fun highlightWeekdayIndex(averages: List<WeekdayAverage>): Int {
        if (averages.isEmpty()) return -1
        var bestIdx = -1
        var best = Long.MIN_VALUE
        averages.forEachIndexed { idx, avg ->
            if (avg.sampleCount <= 0) return@forEachIndexed
            if (avg.earnedMinor > best) {
                best = avg.earnedMinor
                bestIdx = idx
            }
        }
        return bestIdx
    }

    /**
     * Active day = completed session with activeMinutes > 0 that local calendar day.
     * Current streak ends today, or yesterday if today has no activity yet.
     */
    fun streakState(
        sessions: List<SessionEarnings>,
        todayEpochDay: Long,
        zoneId: ZoneId,
    ): StreakState {
        val activeDays = sortedActiveEpochDays(sessions, zoneId)
        if (activeDays.isEmpty()) {
            return StreakState(currentDays = 0, bestDays = 0, lastActiveLocalDateEpochDay = null)
        }
        val activeSet = activeDays.toHashSet()
        val last = activeDays.last()
        val current = currentStreakLength(activeSet, todayEpochDay)
        val best = bestStreakLength(activeDays)
        return StreakState(
            currentDays = current,
            bestDays = best,
            lastActiveLocalDateEpochDay = last,
        )
    }

    fun sortedActiveEpochDays(
        sessions: List<SessionEarnings>,
        zoneId: ZoneId,
    ): List<Long> {
        val days = sortedSetOf<Long>()
        for (item in sessions) {
            if (item.session.status != SessionStatus.COMPLETED) continue
            if (item.breakdown.activeMinutes <= 0L) continue
            days.add(epochDay(item.session.startAt, zoneId))
        }
        return days.toList()
    }

    fun currentStreakLength(activeDays: Set<Long>, todayEpochDay: Long): Int {
        if (activeDays.isEmpty()) return 0
        val anchor = when {
            todayEpochDay in activeDays -> todayEpochDay
            (todayEpochDay - 1) in activeDays -> todayEpochDay - 1
            else -> return 0
        }
        var count = 0
        var day = anchor
        while (day in activeDays) {
            count++
            day--
        }
        return count
    }

    fun bestStreakLength(sortedActiveDays: List<Long>): Int {
        if (sortedActiveDays.isEmpty()) return 0
        var best = 1
        var run = 1
        for (i in 1 until sortedActiveDays.size) {
            if (sortedActiveDays[i] == sortedActiveDays[i - 1] + 1) {
                run++
                if (run > best) best = run
            } else {
                run = 1
            }
        }
        return best
    }

    // --- Achievement evaluation (pure predicates) ---

    data class AchievementEvalInput(
        val sessions: List<SessionEarnings>,
        val hasAnyGoal: Boolean,
        val hasPositiveAllocation: Boolean,
        val streakCurrentDays: Int,
        val lifetimeEarnedMinor: Long,
        val zoneId: ZoneId,
    )

    fun evaluateAchievementIds(input: AchievementEvalInput): Set<String> {
        val unlocked = mutableSetOf<String>()
        val completed = input.sessions.filter { it.session.status == SessionStatus.COMPLETED }
        if (completed.isNotEmpty()) {
            unlocked += com.trackpay.app.domain.model.AchievementIds.FIRST_SHIFT
        }
        for (item in completed) {
            val hour = Instant.ofEpochMilli(item.session.startAt)
                .atZone(input.zoneId)
                .hour
            if (hour < 7) {
                unlocked += com.trackpay.app.domain.model.AchievementIds.EARLY_BIRD
                break
            }
        }
        if (hasWeekWithActiveDays(completed, minDays = 5, zoneId = input.zoneId)) {
            unlocked += com.trackpay.app.domain.model.AchievementIds.WEEK_WARRIOR
        }
        if (completed.any { it.breakdown.otMinutes > 0L }) {
            unlocked += com.trackpay.app.domain.model.AchievementIds.OT_HERO
        }
        if (input.hasAnyGoal) {
            unlocked += com.trackpay.app.domain.model.AchievementIds.GOAL_STARTER
        }
        if (input.hasPositiveAllocation) {
            unlocked += com.trackpay.app.domain.model.AchievementIds.GOAL_FUNDED
        }
        if (input.streakCurrentDays >= 7 ||
            bestStreakLength(sortedActiveEpochDays(completed, input.zoneId)) >= 7
        ) {
            unlocked += com.trackpay.app.domain.model.AchievementIds.STREAK_7
        }
        if (input.lifetimeEarnedMinor >= 100_000L) {
            unlocked += com.trackpay.app.domain.model.AchievementIds.EARNED_1K
        }
        return unlocked
    }

    /**
     * True when any ISO week contains at least [minDays] distinct active local days.
     */
    fun hasWeekWithActiveDays(
        sessions: List<SessionEarnings>,
        minDays: Int,
        zoneId: ZoneId,
    ): Boolean {
        val byWeek = HashMap<Long, HashSet<Long>>()
        for (item in sessions) {
            if (item.session.status != SessionStatus.COMPLETED) continue
            if (item.breakdown.activeMinutes <= 0L) continue
            val day = epochDay(item.session.startAt, zoneId)
            val week = isoWeekStartEpochDay(day)
            byWeek.getOrPut(week) { HashSet() }.add(day)
        }
        return byWeek.values.any { it.size >= minDays }
    }

    fun toSessionEarnings(
        session: WorkSession,
        breaks: List<BreakInterval>,
        nowMillis: Long,
    ): SessionEarnings {
        val breakdown = EarningsCalculator.calculate(session, breaks, nowMillis)
        return SessionEarnings(session = session, breakdown = breakdown)
    }
}
