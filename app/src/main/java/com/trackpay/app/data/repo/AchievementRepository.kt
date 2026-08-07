package com.trackpay.app.data.repo

import com.trackpay.app.data.local.dao.AchievementUnlockDao
import com.trackpay.app.data.local.toDomain
import com.trackpay.app.data.local.toEntity
import com.trackpay.app.domain.model.AchievementCatalog
import com.trackpay.app.domain.model.AchievementStatus
import com.trackpay.app.domain.model.AchievementUnlock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AchievementRepository @Inject constructor(
    private val achievementUnlockDao: AchievementUnlockDao,
) {
    fun observeUnlocks(): Flow<List<AchievementUnlock>> =
        achievementUnlockDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    suspend fun listUnlocks(): List<AchievementUnlock> =
        achievementUnlockDao.listAll().map { it.toDomain() }

    suspend fun listUnlockedIds(): Set<String> =
        achievementUnlockDao.listIds().toSet()

    /**
     * Idempotent unlock. Returns true when a new row was inserted.
     */
    suspend fun unlock(id: String, unlockedAt: Long): Boolean {
        val rows = achievementUnlockDao.insertIgnore(
            AchievementUnlock(id = id, unlockedAt = unlockedAt).toEntity(),
        )
        return rows != -1L
    }

    suspend fun unlockAll(ids: Collection<String>, unlockedAt: Long): List<String> {
        val newly = mutableListOf<String>()
        for (id in ids) {
            if (unlock(id, unlockedAt)) newly += id
        }
        return newly
    }

    fun observeStatuses(): Flow<List<AchievementStatus>> =
        observeUnlocks().map { unlocks ->
            val byId = unlocks.associateBy { it.id }
            AchievementCatalog.ALL.map { def ->
                AchievementStatus(def = def, unlock = byId[def.id])
            }
        }
}
