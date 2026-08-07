package com.trackpay.app.domain.model

enum class SessionStatus {
    RUNNING,
    PAUSED,
    COMPLETED,
    CANCELLED,
}

enum class SessionSource {
    MANUAL,
    NOTIFICATION,
    GEOFENCE,
    EDIT,
}

data class Job(
    val id: String,
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

data class WorkSession(
    val id: String,
    val jobId: String,
    val startAt: Long,
    val endAt: Long?,
    val status: SessionStatus,
    val snapshotHourlyRateMinor: Long,
    val snapshotOtRateMinor: Long?,
    val snapshotOtThresholdMinutes: Int?,
    val notes: String?,
    val source: SessionSource,
)

data class BreakInterval(
    val id: String,
    val sessionId: String,
    val startAt: Long,
    val endAt: Long?,
)

data class EarningsBreakdown(
    val activeMillis: Long,
    val activeMinutes: Long,
    val regularMinutes: Long,
    val otMinutes: Long,
    val earnedMinor: Long,
)

data class ActiveSession(
    val session: WorkSession,
    val job: Job,
    val breaks: List<BreakInterval>,
)

data class SessionListItem(
    val session: WorkSession,
    val jobName: String,
    val jobColorArgb: Int,
    val earnedMinor: Long,
    val activeMillis: Long,
    val regularMinutes: Long,
    val otMinutes: Long,
)

data class SessionDetail(
    val session: WorkSession,
    val job: Job?,
    val breaks: List<BreakInterval>,
    val breakdown: EarningsBreakdown,
)

data class SessionTotals(
    val earnedMinor: Long,
    val shiftCount: Int,
    val activeMillis: Long,
)

data class HistoryFilter(
    val query: String = "",
    val jobId: String? = null,
    val rangeStartMillis: Long? = null,
    val rangeEndExclusiveMillis: Long? = null,
)

/** Input break for create/edit forms. */
data class BreakInput(
    val startAt: Long,
    val endAt: Long,
)

object JobDefaults {
    const val DEFAULT_OT_THRESHOLD_MINUTES: Int = 480
    const val DEFAULT_COLOR_ARGB: Int = 0xFF10B981.toInt()
    const val DEFAULT_ICON_KEY: String = "work"
    const val DEFAULT_RADIUS_METERS: Int = 150
    const val DEFAULT_CURRENCY_CODE: String = "USD"
    val COLOR_PRESETS: IntArray = intArrayOf(
        DEFAULT_COLOR_ARGB,       // emerald
        0xFF3B82F6.toInt(),       // blue
        0xFFF59E0B.toInt(),       // amber
        0xFFEF4444.toInt(),       // red
        0xFF8B5CF6.toInt(),       // violet
        0xFFEC4899.toInt(),       // pink
    )

    val ICON_PRESETS: List<String> = listOf(
        "work",
        "store",
        "local_shipping",
        "restaurant",
        "laptop",
        "construction",
    )
}

enum class GoalStatus {
    ACTIVE,
    COMPLETED,
    ARCHIVED,
}

data class Goal(
    val id: String,
    val name: String,
    val targetMinor: Long,
    val deadlineEpochDay: Long,
    val iconKey: String,
    val colorArgb: Int,
    val allocationBps: Int,
    val status: GoalStatus,
    val createdAt: Long,
    val sortOrder: Int?,
)

data class GoalAllocation(
    val id: String,
    val goalId: String,
    val sessionId: String,
    val amountMinor: Long,
    val createdAt: Long,
)

data class GoalProgress(
    val goal: Goal,
    val savedMinor: Long,
    val remainingMinor: Long,
    val progress: Float,
    val pacePerWeekMinor: Long,
    val overdue: Boolean,
)

data class GoalTemplate(
    val name: String,
    val iconKey: String,
    val defaultTargetMinor: Long,
    val defaultHorizonMonths: Int,
    val colorArgb: Int = GoalDefaults.DEFAULT_COLOR_ARGB,
)

object GoalDefaults {
    const val BPS_DENOMINATOR: Int = 10_000
    const val DEFAULT_COLOR_ARGB: Int = 0xFF10B981.toInt()
    const val DEFAULT_ICON_KEY: String = "savings"
    const val DEFAULT_ALLOCATION_BPS: Int = 0

    val COLOR_PRESETS: IntArray = intArrayOf(
        DEFAULT_COLOR_ARGB,
        0xFF3B82F6.toInt(),
        0xFFF59E0B.toInt(),
        0xFFEF4444.toInt(),
        0xFF8B5CF6.toInt(),
        0xFFEC4899.toInt(),
    )

    val TEMPLATES: List<GoalTemplate> = listOf(
        GoalTemplate(
            name = "Emergency Fund",
            iconKey = "shield",
            defaultTargetMinor = 300_000L,
            defaultHorizonMonths = 6,
            colorArgb = 0xFF10B981.toInt(),
        ),
        GoalTemplate(
            name = "Vacation",
            iconKey = "flight",
            defaultTargetMinor = 250_000L,
            defaultHorizonMonths = 4,
            colorArgb = 0xFF3B82F6.toInt(),
        ),
        GoalTemplate(
            name = "New Car",
            iconKey = "directions_car",
            defaultTargetMinor = 600_000L,
            defaultHorizonMonths = 12,
            colorArgb = 0xFFF59E0B.toInt(),
        ),
        GoalTemplate(
            name = "Home Fund",
            iconKey = "home",
            defaultTargetMinor = 5_000_000L,
            defaultHorizonMonths = 48,
            colorArgb = 0xFF8B5CF6.toInt(),
        ),
        GoalTemplate(
            name = "New Laptop",
            iconKey = "laptop",
            defaultTargetMinor = 200_000L,
            defaultHorizonMonths = 4,
            colorArgb = 0xFFEC4899.toInt(),
        ),
    )
}
