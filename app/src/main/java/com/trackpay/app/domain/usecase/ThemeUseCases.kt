package com.trackpay.app.domain.usecase

import com.trackpay.app.data.local.PreferencesDataSource
import com.trackpay.app.data.repo.InsightsRepository
import com.trackpay.app.domain.model.ThemeCatalog
import com.trackpay.app.domain.model.ThemeOwnership
import com.trackpay.app.domain.model.ThemePackUi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

/** Active theme id from DataStore (default verdant). */
@Singleton
class ObserveActiveThemeIdUseCase @Inject constructor(
    private val preferences: PreferencesDataSource,
) {
    operator fun invoke(): Flow<String> = preferences.activeThemeId
}

/**
 * Catalog rows with Model A ownership derived from lifetime earned + active id.
 */
@Singleton
class ObserveThemesUseCase @Inject constructor(
    private val insightsRepository: InsightsRepository,
    private val preferences: PreferencesDataSource,
) {
    operator fun invoke(): Flow<List<ThemePackUi>> =
        combine(
            insightsRepository.observeLifetimeEarned(),
            preferences.activeThemeId,
        ) { lifetimeEarnedMinor, activeId ->
            ThemeCatalog.entries.map { entry ->
                ThemePackUi(
                    id = entry.id,
                    name = entry.name,
                    unlockMinor = entry.unlockMinor,
                    owned = ThemeOwnership.isOwned(entry, lifetimeEarnedMinor),
                    active = entry.id == activeId,
                )
            }
        }.distinctUntilChanged()
}

/**
 * Applies [themeId] when owned under Model A. Writes DataStore only on success.
 * @return true if applied, false if unknown or locked.
 */
@Singleton
class ApplyThemeUseCase @Inject constructor(
    private val insightsRepository: InsightsRepository,
    private val preferences: PreferencesDataSource,
) {
    suspend operator fun invoke(themeId: String): Boolean {
        val entry = ThemeCatalog.entry(themeId) ?: return false
        val lifetime = insightsRepository.lifetimeEarnedMinor()
        if (!ThemeOwnership.isOwned(entry, lifetime)) return false
        preferences.setActiveThemeId(entry.id)
        return true
    }
}

/**
 * Wallet display = lifetime tracked earnings (cosmetic; no debit).
 * Alias over [ObserveLifetimeEarnedUseCase] for themes UI naming.
 */
@Singleton
class ObserveWalletUseCase @Inject constructor(
    private val observeLifetimeEarned: ObserveLifetimeEarnedUseCase,
) {
    operator fun invoke(): Flow<Long> = observeLifetimeEarned()
}
