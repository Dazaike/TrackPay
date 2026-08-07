package com.trackpay.app.data.local

import com.trackpay.app.data.local.entity.BreakIntervalEntity
import com.trackpay.app.data.local.entity.GoalAllocationEntity
import com.trackpay.app.data.local.entity.GoalEntity
import com.trackpay.app.data.local.entity.JobEntity
import com.trackpay.app.data.local.entity.WorkSessionEntity
import com.trackpay.app.data.local.entity.AchievementUnlockEntity

import com.trackpay.app.domain.model.BreakInterval
import com.trackpay.app.domain.model.Goal
import com.trackpay.app.domain.model.GoalAllocation
import com.trackpay.app.domain.model.GoalStatus
import com.trackpay.app.domain.model.Job
import com.trackpay.app.domain.model.SessionSource
import com.trackpay.app.domain.model.SessionStatus
import com.trackpay.app.domain.model.WorkSession
import com.trackpay.app.domain.model.AchievementUnlock


fun JobEntity.toDomain(): Job =
    Job(
        id = id,
        name = name,
        hourlyRateMinor = hourlyRateMinor,
        otRateMinor = otRateMinor,
        otThresholdMinutes = otThresholdMinutes,
        colorArgb = colorArgb,
        iconKey = iconKey,
        archived = archived,
        createdAt = createdAt,
    )

fun Job.toEntity(): JobEntity =
    JobEntity(
        id = id,
        name = name,
        hourlyRateMinor = hourlyRateMinor,
        otRateMinor = otRateMinor,
        otThresholdMinutes = otThresholdMinutes,
        colorArgb = colorArgb,
        iconKey = iconKey,
        archived = archived,
        createdAt = createdAt,
    )

fun WorkSessionEntity.toDomain(): WorkSession =
    WorkSession(
        id = id,
        jobId = jobId,
        startAt = startAt,
        endAt = endAt,
        status = SessionStatus.valueOf(status),
        snapshotHourlyRateMinor = snapshotHourlyRateMinor,
        snapshotOtRateMinor = snapshotOtRateMinor,
        snapshotOtThresholdMinutes = snapshotOtThresholdMinutes,
        notes = notes,
        source = runCatching { SessionSource.valueOf(source) }.getOrDefault(SessionSource.MANUAL),
    )

fun WorkSession.toEntity(): WorkSessionEntity =
    WorkSessionEntity(
        id = id,
        jobId = jobId,
        startAt = startAt,
        endAt = endAt,
        status = status.name,
        snapshotHourlyRateMinor = snapshotHourlyRateMinor,
        snapshotOtRateMinor = snapshotOtRateMinor,
        snapshotOtThresholdMinutes = snapshotOtThresholdMinutes,
        notes = notes,
        source = source.name,
    )

fun BreakIntervalEntity.toDomain(): BreakInterval =
    BreakInterval(
        id = id,
        sessionId = sessionId,
        startAt = startAt,
        endAt = endAt,
    )

fun BreakInterval.toEntity(): BreakIntervalEntity =
    BreakIntervalEntity(
        id = id,
        sessionId = sessionId,
        startAt = startAt,
        endAt = endAt,
    )

fun GoalEntity.toDomain(): Goal =
    Goal(
        id = id,
        name = name,
        targetMinor = targetMinor,
        deadlineEpochDay = deadlineEpochDay,
        iconKey = iconKey,
        colorArgb = colorArgb,
        allocationBps = allocationBps,
        status = runCatching { GoalStatus.valueOf(status) }.getOrDefault(GoalStatus.ACTIVE),
        createdAt = createdAt,
        sortOrder = sortOrder,
    )

fun Goal.toEntity(): GoalEntity =
    GoalEntity(
        id = id,
        name = name,
        targetMinor = targetMinor,
        deadlineEpochDay = deadlineEpochDay,
        iconKey = iconKey,
        colorArgb = colorArgb,
        allocationBps = allocationBps,
        status = status.name,
        createdAt = createdAt,
        sortOrder = sortOrder,
    )

fun GoalAllocationEntity.toDomain(): GoalAllocation =
    GoalAllocation(
        id = id,
        goalId = goalId,
        sessionId = sessionId,
        amountMinor = amountMinor,
        createdAt = createdAt,
    )

fun GoalAllocation.toEntity(): GoalAllocationEntity =
    GoalAllocationEntity(
        id = id,
        goalId = goalId,
        sessionId = sessionId,
        amountMinor = amountMinor,
        createdAt = createdAt,
    )

fun AchievementUnlockEntity.toDomain(): AchievementUnlock =
    AchievementUnlock(
        id = id,
        unlockedAt = unlockedAt,
    )

fun AchievementUnlock.toEntity(): AchievementUnlockEntity =
    AchievementUnlockEntity(
        id = id,
        unlockedAt = unlockedAt,
    )
