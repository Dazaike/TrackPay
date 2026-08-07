package com.trackpay.app.data.local

import com.trackpay.app.data.local.entity.BreakIntervalEntity
import com.trackpay.app.data.local.entity.JobEntity
import com.trackpay.app.data.local.entity.WorkSessionEntity
import com.trackpay.app.domain.model.BreakInterval
import com.trackpay.app.domain.model.Job
import com.trackpay.app.domain.model.SessionSource
import com.trackpay.app.domain.model.SessionStatus
import com.trackpay.app.domain.model.WorkSession

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
