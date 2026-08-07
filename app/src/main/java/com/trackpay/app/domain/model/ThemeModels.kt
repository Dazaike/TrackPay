package com.trackpay.app.domain.model

object ThemeIds {
    const val CLASSIC_BLUE = "classic_blue"
    const val VERDANT = "verdant"
    const val SUNSET = "sunset"
    const val BERRY = "berry"
    const val LAGOON = "lagoon"
    const val AMETHYST = "amethyst"
    const val EMBER = "ember"

    /** Default active theme for fresh installs. */
    const val DEFAULT = VERDANT

    val FREE_IDS: Set<String> = setOf(CLASSIC_BLUE, VERDANT)
}

data class ThemeCatalogEntry(
    val id: String,
    val name: String,
    val unlockMinor: Long,
)

/**
 * In-code theme catalog (Model A thresholds).
 * Free: classic_blue + verdant (default active). Paid gates start at sunset.
 */
object ThemeCatalog {
    val entries: List<ThemeCatalogEntry> = listOf(
        ThemeCatalogEntry(ThemeIds.CLASSIC_BLUE, "Classic Blue", 0L),
        ThemeCatalogEntry(ThemeIds.VERDANT, "Verdant", 0L),
        ThemeCatalogEntry(ThemeIds.SUNSET, "Sunset", 5_000L),
        ThemeCatalogEntry(ThemeIds.BERRY, "Berry", 10_000L),
        ThemeCatalogEntry(ThemeIds.LAGOON, "Lagoon", 25_000L),
        ThemeCatalogEntry(ThemeIds.AMETHYST, "Amethyst", 50_000L),
        ThemeCatalogEntry(ThemeIds.EMBER, "Ember", 100_000L),
    )

    private val byId: Map<String, ThemeCatalogEntry> = entries.associateBy { it.id }

    fun entry(id: String): ThemeCatalogEntry? = byId[id]

    fun requires(id: String): ThemeCatalogEntry =
        entry(id) ?: error("Unknown theme id: $id")
}

/**
 * Model A ownership: permanent once lifetime earned crosses unlock threshold.
 * Free themes (unlockMinor == 0) are always owned.
 */
object ThemeOwnership {
    fun isOwned(unlockMinor: Long, lifetimeEarnedMinor: Long): Boolean =
        unlockMinor <= 0L || lifetimeEarnedMinor >= unlockMinor

    fun isOwned(entry: ThemeCatalogEntry, lifetimeEarnedMinor: Long): Boolean =
        isOwned(entry.unlockMinor, lifetimeEarnedMinor)

    fun isOwned(themeId: String, lifetimeEarnedMinor: Long): Boolean {
        val entry = ThemeCatalog.entry(themeId) ?: return false
        return isOwned(entry, lifetimeEarnedMinor)
    }
}

data class ThemePackUi(
    val id: String,
    val name: String,
    val unlockMinor: Long,
    val owned: Boolean,
    val active: Boolean,
)
