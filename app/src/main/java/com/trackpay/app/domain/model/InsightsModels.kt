package com.trackpay.app.domain.model

/**
 * Rolling local-TZ insight windows.
 */
enum class InsightsRange(val dayCount: Int) {
    D7(7),
    D30(30),
    D90(90),
    Y1(365),
}

enum class InsightsMetric {
    EARNINGS,
    HOURS,
}

/**
 * ISO Monday-start week challenge. [targetMinor] is previous week completed sum,
 * or fallback max(10000, primaryHourly * 20h) when previous week is empty.
 */
data class WeeklyChallenge(
    val weekStartEpochDay: Long,
    val targetMinor: Long,
    val earnedMinor: Long,
    val daysLeft: Int,
)

data class RangeBucket(
    val startEpochDay: Long,
    val label: String,
    val earnedMinor: Long,
    val activeMillis: Long,
)

data class InsightsRangeSummary(
    val earnedMinor: Long,
    val activeMillis: Long,
    val buckets: List<RangeBucket>,
)

/** ISO dayOfWeek: 1 = Monday … 7 = Sunday. */
data class WeekdayAverage(
    val dayOfWeek: Int,
    val earnedMinor: Long,
    val activeMillis: Long,
    val sampleCount: Int,
)

data class StreakState(
    val currentDays: Int,
    val bestDays: Int,
    val lastActiveLocalDateEpochDay: Long?,
)

data class AchievementDef(
    val id: String,
    val title: String,
    val description: String,
)

data class AchievementUnlock(
    val id: String,
    val unlockedAt: Long,
)

data class AchievementStatus(
    val def: AchievementDef,
    val unlock: AchievementUnlock?,
) {
    val unlocked: Boolean get() = unlock != null
}

/**
 * Combined Insights tab snapshot for one-shot observation.
 */
data class InsightsSnapshot(
    val weeklyChallenge: WeeklyChallenge,
    val rangeSummary: InsightsRangeSummary,
    val weekdayAverages: List<WeekdayAverage>,
    val streak: StreakState,
    val achievements: List<AchievementStatus>,
    val lifetimeEarnedMinor: Long,
    val hasCompletedSessions: Boolean,
)

object AchievementIds {
    const val FIRST_SHIFT = "first_shift"
    const val EARLY_BIRD = "early_bird"
    const val WEEK_WARRIOR = "week_warrior"
    const val OT_HERO = "ot_hero"
    const val GOAL_STARTER = "goal_starter"
    const val GOAL_FUNDED = "goal_funded"
    const val STREAK_7 = "streak_7"
    const val EARNED_1K = "earned_1k"
}

object AchievementCatalog {
    val ALL: List<AchievementDef> = listOf(
        AchievementDef(
            id = AchievementIds.FIRST_SHIFT,
            title = "First clock-out",
            description = "Complete your first shift",
        ),
        AchievementDef(
            id = AchievementIds.EARLY_BIRD,
            title = "Early bird",
            description = "Start a shift before 7 local",
        ),
        AchievementDef(
            id = AchievementIds.WEEK_WARRIOR,
            title = "Week warrior",
            description = "Work 5 active days in one ISO week",
        ),
        AchievementDef(
            id = AchievementIds.OT_HERO,
            title = "OT hero",
            description = "Log overtime on a shift",
        ),
        AchievementDef(
            id = AchievementIds.GOAL_STARTER,
            title = "Goal starter",
            description = "Create a savings goal",
        ),
        AchievementDef(
            id = AchievementIds.GOAL_FUNDED,
            title = "Goal funded",
            description = "Receive your first goal allocation",
        ),
        AchievementDef(
            id = AchievementIds.STREAK_7,
            title = "Week streak",
            description = "Hit a 7-day work streak",
        ),
        AchievementDef(
            id = AchievementIds.EARNED_1K,
            title = "First grand",
            description = "Earn $1,000 lifetime",
        ),
    )

    private val byId: Map<String, AchievementDef> = ALL.associateBy { it.id }

    fun def(id: String): AchievementDef? = byId[id]
}
