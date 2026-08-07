package com.trackpay.app.domain.usecase

import com.trackpay.app.domain.model.ThemeCatalog
import com.trackpay.app.domain.model.ThemeIds
import com.trackpay.app.domain.model.ThemeOwnership
import com.trackpay.app.domain.model.ThemePackUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure Model A unlock / apply-gate tests (no Android runtime).
 */
class ThemeUnlockTest {

    @Test
    fun free_themes_always_owned_at_zero_lifetime() {
        assertTrue(ThemeOwnership.isOwned(ThemeIds.CLASSIC_BLUE, 0L))
        assertTrue(ThemeOwnership.isOwned(ThemeIds.VERDANT, 0L))
        assertTrue(ThemeOwnership.isOwned(0L, 0L))
    }

    @Test
    fun paid_themes_locked_below_threshold() {
        assertFalse(ThemeOwnership.isOwned(ThemeIds.SUNSET, 4_999L))
        assertFalse(ThemeOwnership.isOwned(ThemeIds.BERRY, 9_999L))
        assertFalse(ThemeOwnership.isOwned(ThemeIds.LAGOON, 24_999L))
        assertFalse(ThemeOwnership.isOwned(ThemeIds.AMETHYST, 49_999L))
        assertFalse(ThemeOwnership.isOwned(ThemeIds.EMBER, 99_999L))
    }

    @Test
    fun ownership_flips_at_exact_threshold() {
        assertTrue(ThemeOwnership.isOwned(ThemeIds.SUNSET, 5_000L))
        assertTrue(ThemeOwnership.isOwned(ThemeIds.BERRY, 10_000L))
        assertTrue(ThemeOwnership.isOwned(ThemeIds.LAGOON, 25_000L))
        assertTrue(ThemeOwnership.isOwned(ThemeIds.AMETHYST, 50_000L))
        assertTrue(ThemeOwnership.isOwned(ThemeIds.EMBER, 100_000L))
    }

    @Test
    fun higher_lifetime_keeps_lower_tiers_owned() {
        val life = 100_000L
        for (entry in ThemeCatalog.entries) {
            assertTrue(
                "${entry.id} should be owned at $life",
                ThemeOwnership.isOwned(entry, life),
            )
        }
    }

    @Test
    fun unknown_theme_id_not_owned() {
        assertFalse(ThemeOwnership.isOwned("not_a_theme", 1_000_000L))
    }

    @Test
    fun catalog_free_ids_match_contract() {
        val free = ThemeCatalog.entries.filter { it.unlockMinor <= 0L }.map { it.id }.toSet()
        assertEquals(ThemeIds.FREE_IDS, free)
        assertEquals(0L, ThemeCatalog.requires(ThemeIds.VERDANT).unlockMinor)
        assertEquals(0L, ThemeCatalog.requires(ThemeIds.CLASSIC_BLUE).unlockMinor)
    }

    @Test
    fun catalog_paid_thresholds_match_contract() {
        assertEquals(5_000L, ThemeCatalog.requires(ThemeIds.SUNSET).unlockMinor)
        assertEquals(10_000L, ThemeCatalog.requires(ThemeIds.BERRY).unlockMinor)
        assertEquals(25_000L, ThemeCatalog.requires(ThemeIds.LAGOON).unlockMinor)
        assertEquals(50_000L, ThemeCatalog.requires(ThemeIds.AMETHYST).unlockMinor)
        assertEquals(100_000L, ThemeCatalog.requires(ThemeIds.EMBER).unlockMinor)
    }

    @Test
    fun default_active_is_verdant() {
        assertEquals(ThemeIds.VERDANT, ThemeIds.DEFAULT)
    }

    @Test
    fun apply_gate_allows_free_rejects_locked() {
        // Mirrors ApplyThemeUseCase ownership check without DataStore.
        fun canApply(themeId: String, lifetime: Long): Boolean {
            val entry = ThemeCatalog.entry(themeId) ?: return false
            return ThemeOwnership.isOwned(entry, lifetime)
        }

        assertTrue(canApply(ThemeIds.VERDANT, 0L))
        assertTrue(canApply(ThemeIds.CLASSIC_BLUE, 0L))
        assertFalse(canApply(ThemeIds.SUNSET, 0L))
        assertFalse(canApply(ThemeIds.SUNSET, 4_999L))
        assertTrue(canApply(ThemeIds.SUNSET, 5_000L))
        assertFalse(canApply("missing", 999_999L))
    }

    @Test
    fun observe_projection_marks_owned_and_active() {
        val lifetime = 10_000L
        val activeId = ThemeIds.BERRY
        val rows = ThemeCatalog.entries.map { entry ->
            ThemePackUi(
                id = entry.id,
                name = entry.name,
                unlockMinor = entry.unlockMinor,
                owned = ThemeOwnership.isOwned(entry, lifetime),
                active = entry.id == activeId,
            )
        }

        val byId = rows.associateBy { it.id }
        assertTrue(byId.getValue(ThemeIds.VERDANT).owned)
        assertTrue(byId.getValue(ThemeIds.CLASSIC_BLUE).owned)
        assertTrue(byId.getValue(ThemeIds.SUNSET).owned)
        assertTrue(byId.getValue(ThemeIds.BERRY).owned)
        assertFalse(byId.getValue(ThemeIds.LAGOON).owned)
        assertTrue(byId.getValue(ThemeIds.BERRY).active)
        assertFalse(byId.getValue(ThemeIds.VERDANT).active)
        assertEquals(1, rows.count { it.active })
    }
}
