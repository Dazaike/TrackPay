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
