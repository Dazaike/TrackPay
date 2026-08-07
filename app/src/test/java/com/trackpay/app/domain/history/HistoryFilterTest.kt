package com.trackpay.app.domain.history

import com.trackpay.app.domain.calc.EarningsCalculator
import com.trackpay.app.domain.model.BreakInput
import com.trackpay.app.domain.model.BreakInterval
import com.trackpay.app.domain.model.HistoryFilter
import com.trackpay.app.domain.model.Job
import com.trackpay.app.domain.model.JobDefaults
import com.trackpay.app.domain.model.SessionSource
import com.trackpay.app.domain.model.SessionStatus
import com.trackpay.app.domain.model.WorkSession
import com.trackpay.app.domain.usecase.applyHistoryFilter
import com.trackpay.app.domain.usecase.computeSessionTotals
import com.trackpay.app.domain.usecase.resolveEditSnapshots
import com.trackpay.app.domain.usecase.validateCompletedSessionInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryFilterTest {

    private val day0 = 1_700_000_000_000L
    private val hour = 3_600_000L

    private val jobA = Job(
        id = "job-a",
        name = "Cafe",
        hourlyRateMinor = 2_500L,
        otRateMinor = null,
        otThresholdMinutes = null,
        colorArgb = JobDefaults.DEFAULT_COLOR_ARGB,
        iconKey = JobDefaults.DEFAULT_ICON_KEY,
        archived = false,
        createdAt = 1L,
    )

    private val jobB = Job(
        id = "job-b",
        name = "Warehouse",
        hourlyRateMinor = 3_000L,
        otRateMinor = 4_500L,
        otThresholdMinutes = 480,
        colorArgb = 0xFF3B82F6.toInt(),
        iconKey = "store",
        archived = false,
        createdAt = 2L,
    )

    private fun session(
        id: String,
        jobId: String,
        startAt: Long,
        endAt: Long,
        hourly: Long = 2_500L,
        ot: Long? = null,
        otThreshold: Int? = null,
        notes: String? = null,
    ) = WorkSession(
        id = id,
        jobId = jobId,
        startAt = startAt,
        endAt = endAt,
        status = SessionStatus.COMPLETED,
        snapshotHourlyRateMinor = hourly,
        snapshotOtRateMinor = ot,
        snapshotOtThresholdMinutes = otThreshold,
        notes = notes,
        source = SessionSource.MANUAL,
    )

    private val jobsById = mapOf(jobA.id to jobA, jobB.id to jobB)

    private val sessions = listOf(
        session("s1", jobA.id, day0, day0 + hour, notes = "Morning rush"),
        session("s2", jobB.id, day0 + 2 * hour, day0 + 4 * hour, hourly = 3_000L, notes = "Loading dock"),
        session("s3", jobA.id, day0 + 10 * hour, day0 + 12 * hour, notes = "Evening close"),
    )

    @Test
    fun filter_by_job_and_range() {
        val filter = HistoryFilter(
            jobId = jobA.id,
            rangeStartMillis = day0 + hour,
            rangeEndExclusiveMillis = day0 + 20 * hour,
        )
        val items = applyHistoryFilter(sessions, jobsById, filter)
        assertEquals(1, items.size)
        assertEquals("s3", items.single().session.id)
        assertEquals("Cafe", items.single().jobName)
    }

    @Test
    fun search_notes_case_insensitive() {
        val filter = HistoryFilter(query = "RUSH")
        val items = applyHistoryFilter(sessions, jobsById, filter)
        assertEquals(1, items.size)
        assertEquals("s1", items.single().session.id)
    }

    @Test
    fun search_job_name() {
        val filter = HistoryFilter(query = "ware")
        val items = applyHistoryFilter(sessions, jobsById, filter)
        assertEquals(1, items.size)
        assertEquals("s2", items.single().session.id)
        assertEquals("Warehouse", items.single().jobName)
    }

    @Test
    fun edit_start_end_changes_earned_minor() {
        val base = session("e1", jobA.id, day0, day0 + hour, hourly = 2_500L)
        val breaks = emptyList<BreakInterval>()
        val oneHour = EarningsCalculator.calculate(base, breaks, base.endAt!!)
        assertEquals(2_500L, oneHour.earnedMinor)

        val twoHours = base.copy(endAt = day0 + 2 * hour)
        val earnedTwo = EarningsCalculator.calculate(twoHours, breaks, twoHours.endAt!!)
        assertEquals(5_000L, earnedTwo.earnedMinor)
        assertTrue(earnedTwo.earnedMinor > oneHour.earnedMinor)

        val shortened = base.copy(endAt = day0 + hour / 2)
        val earnedHalf = EarningsCalculator.calculate(shortened, breaks, shortened.endAt!!)
        assertEquals(1_250L, earnedHalf.earnedMinor)
    }

    @Test
    fun manual_create_validation_end_before_start_fails() {
        val err = validateCompletedSessionInput(
            startAt = day0 + hour,
            endAt = day0,
            breaks = emptyList(),
        )
        assertEquals("End must be after start", err)
    }

    @Test
    fun manual_create_validation_break_outside_session_fails() {
        val err = validateCompletedSessionInput(
            startAt = day0,
            endAt = day0 + 2 * hour,
            breaks = listOf(BreakInput(day0 - 1, day0 + 10_000L)),
        )
        assertNotNull(err)
        assertTrue(err!!.contains("within"))
    }

    @Test
    fun manual_create_validation_ok() {
        assertNull(
            validateCompletedSessionInput(
                startAt = day0,
                endAt = day0 + 2 * hour,
                breaks = listOf(BreakInput(day0 + 30 * 60_000L, day0 + 45 * 60_000L)),
            ),
        )
    }

    @Test
    fun snapshot_keep_vs_apply_current_rates() {
        val existing = session(
            id = "snap",
            jobId = jobA.id,
            startAt = day0,
            endAt = day0 + hour,
            hourly = 2_000L,
            ot = null,
            otThreshold = null,
        )
        // Job rates changed since session was saved.
        val currentJob = jobA.copy(hourlyRateMinor = 4_000L, otRateMinor = 6_000L, otThresholdMinutes = 480)

        val kept = resolveEditSnapshots(existing, currentJob, applyCurrentJobRates = false)
        assertEquals(2_000L, kept.first)
        assertNull(kept.second)
        assertNull(kept.third)

        val applied = resolveEditSnapshots(existing, currentJob, applyCurrentJobRates = true)
        assertEquals(4_000L, applied.first)
        assertEquals(6_000L, applied.second)
        assertEquals(480, applied.third)

        val withKept = existing.copy(
            snapshotHourlyRateMinor = kept.first,
            snapshotOtRateMinor = kept.second,
            snapshotOtThresholdMinutes = kept.third,
        )
        val withApplied = existing.copy(
            snapshotHourlyRateMinor = applied.first,
            snapshotOtRateMinor = applied.second,
            snapshotOtThresholdMinutes = applied.third,
        )
        val earnedKept = EarningsCalculator.calculate(withKept, emptyList(), existing.endAt!!).earnedMinor
        val earnedApplied = EarningsCalculator.calculate(withApplied, emptyList(), existing.endAt!!).earnedMinor
        assertEquals(2_000L, earnedKept)
        assertEquals(4_000L, earnedApplied)
    }

    @Test
    fun totals_sum_filtered_items() {
        val items = applyHistoryFilter(sessions, jobsById, HistoryFilter())
        val totals = computeSessionTotals(items)
        assertEquals(3, totals.shiftCount)
        assertEquals(items.sumOf { it.earnedMinor }, totals.earnedMinor)
        assertEquals(items.sumOf { it.activeMillis }, totals.activeMillis)
        // s1: 1h @ 2500, s2: 2h @ 3000, s3: 2h @ 2500
        assertEquals(2_500L + 6_000L + 5_000L, totals.earnedMinor)
    }

    @Test
    fun list_item_includes_job_color_and_active_millis() {
        val items = applyHistoryFilter(
            sessions = listOf(sessions[1]),
            jobsById = jobsById,
            filter = HistoryFilter(),
        )
        val item = items.single()
        assertEquals(jobB.colorArgb, item.jobColorArgb)
        assertEquals(2 * hour, item.activeMillis)
        assertEquals(6_000L, item.earnedMinor)
    }
}
