package com.trackpay.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "jobs")
data class JobEntity(
    @PrimaryKey val id: String,
    val name: String,
    val hourlyRateMinor: Long,
    val otRateMinor: Long?,
    val otThresholdMinutes: Int?,
    val colorArgb: Int,
    val iconKey: String,
    val archived: Boolean,
    val createdAt: Long,
)

@Entity(
    tableName = "work_sessions",
    foreignKeys = [
        ForeignKey(
            entity = JobEntity::class,
            parentColumns = ["id"],
            childColumns = ["jobId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index("jobId"),
        Index("status"),
        Index(value = ["status", "startAt"]),
    ],
)
data class WorkSessionEntity(
    @PrimaryKey val id: String,
    val jobId: String,
    val startAt: Long,
    val endAt: Long?,
    val status: String,
    val snapshotHourlyRateMinor: Long,
    val snapshotOtRateMinor: Long?,
    val snapshotOtThresholdMinutes: Int?,
    val notes: String?,
    val source: String,
)

@Entity(
    tableName = "break_intervals",
    foreignKeys = [
        ForeignKey(
            entity = WorkSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId")],
)
data class BreakIntervalEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val startAt: Long,
    val endAt: Long?,
)
