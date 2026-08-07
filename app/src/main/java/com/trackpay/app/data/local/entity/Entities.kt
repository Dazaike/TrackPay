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
    val geoEnabled: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radiusMeters: Int? = null,
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

@Entity(
    tableName = "goals",
    indices = [
        Index("status"),
        Index(value = ["status", "sortOrder"]),
    ],
)
data class GoalEntity(
    @PrimaryKey val id: String,
    val name: String,
    val targetMinor: Long,
    val deadlineEpochDay: Long,
    val iconKey: String,
    val colorArgb: Int,
    val allocationBps: Int,
    val status: String,
    val createdAt: Long,
    val sortOrder: Int?,
)

@Entity(
    tableName = "goal_allocations",
    foreignKeys = [
        ForeignKey(
            entity = GoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("goalId"),
        Index("sessionId"),
        Index(value = ["goalId", "sessionId"], unique = true),
    ],
)
data class GoalAllocationEntity(
    @PrimaryKey val id: String,
    val goalId: String,
    val sessionId: String,
    val amountMinor: Long,
    val createdAt: Long,
)

@Entity(tableName = "achievement_unlocks")
data class AchievementUnlockEntity(
    @PrimaryKey val id: String,
    val unlockedAt: Long,
)
