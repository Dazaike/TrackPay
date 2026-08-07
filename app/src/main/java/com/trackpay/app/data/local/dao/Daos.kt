package com.trackpay.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.trackpay.app.data.local.entity.BreakIntervalEntity
import com.trackpay.app.data.local.entity.AchievementUnlockEntity

import com.trackpay.app.data.local.entity.GoalAllocationEntity
import com.trackpay.app.data.local.entity.GoalEntity
import com.trackpay.app.data.local.entity.JobEntity
import com.trackpay.app.data.local.entity.WorkSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JobDao {
    @Query("SELECT * FROM jobs WHERE archived = 0 ORDER BY name COLLATE NOCASE ASC")
    fun observeActiveJobs(): Flow<List<JobEntity>>

    @Query("SELECT * FROM jobs WHERE archived = 0 ORDER BY name COLLATE NOCASE ASC")
    suspend fun listActiveJobs(): List<JobEntity>

    @Query("SELECT * FROM jobs WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): JobEntity?

    @Query("SELECT * FROM jobs WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<JobEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(job: JobEntity)

    @Update
    suspend fun update(job: JobEntity)

    @Query("UPDATE jobs SET archived = 1 WHERE id = :id")
    suspend fun archive(id: String)
}

@Dao
interface WorkSessionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(session: WorkSessionEntity)

    @Update
    suspend fun update(session: WorkSessionEntity)

    @Query("SELECT * FROM work_sessions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): WorkSessionEntity?

    @Query(
        """
        SELECT * FROM work_sessions
        WHERE status IN ('RUNNING', 'PAUSED')
        ORDER BY startAt DESC
        LIMIT 1
        """,
    )
    suspend fun getActive(): WorkSessionEntity?

    @Query(
        """
        SELECT * FROM work_sessions
        WHERE status IN ('RUNNING', 'PAUSED')
        ORDER BY startAt DESC
        LIMIT 1
        """,
    )
    fun observeActive(): Flow<WorkSessionEntity?>

    @Query(
        """
        SELECT * FROM work_sessions
        WHERE status = 'COMPLETED'
        ORDER BY startAt DESC
        """,
    )
    fun observeCompleted(): Flow<List<WorkSessionEntity>>

    @Query(
        """
        SELECT * FROM work_sessions
        WHERE status = 'COMPLETED'
          AND startAt >= :fromInclusive
          AND startAt < :toExclusive
        ORDER BY startAt DESC
        """,
    )
    suspend fun listCompletedBetween(fromInclusive: Long, toExclusive: Long): List<WorkSessionEntity>

    @Query("DELETE FROM work_sessions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: WorkSessionEntity)

    @Query(
        """
        SELECT * FROM work_sessions
        WHERE status = 'COMPLETED'
        ORDER BY startAt DESC
        """,
    )
    suspend fun listAllCompleted(): List<WorkSessionEntity>
}

@Dao
interface BreakIntervalDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(breakInterval: BreakIntervalEntity)

    @Update
    suspend fun update(breakInterval: BreakIntervalEntity)

    @Query("SELECT * FROM break_intervals WHERE sessionId = :sessionId ORDER BY startAt ASC")
    suspend fun listForSession(sessionId: String): List<BreakIntervalEntity>

    @Query("SELECT * FROM break_intervals WHERE sessionId = :sessionId ORDER BY startAt ASC")
    fun observeForSession(sessionId: String): Flow<List<BreakIntervalEntity>>

    @Query(
        """
        SELECT * FROM break_intervals
        WHERE sessionId = :sessionId AND endAt IS NULL
        ORDER BY startAt DESC
        LIMIT 1
        """,
    )
    suspend fun getOpenBreak(sessionId: String): BreakIntervalEntity?

    @Query("DELETE FROM break_intervals WHERE sessionId = :sessionId")
    suspend fun deleteForSession(sessionId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(breaks: List<BreakIntervalEntity>)

    @Query(
        """
        SELECT * FROM break_intervals
        WHERE sessionId IN (:sessionIds)
        ORDER BY startAt ASC
        """,
    )
    suspend fun listForSessions(sessionIds: List<String>): List<BreakIntervalEntity>

    @Query("SELECT * FROM break_intervals ORDER BY startAt ASC")
    fun observeAll(): Flow<List<BreakIntervalEntity>>

    @Query("SELECT * FROM break_intervals ORDER BY startAt ASC")
    suspend fun listAll(): List<BreakIntervalEntity>
}

@Dao
interface GoalDao {
    @Query(
        """
        SELECT * FROM goals
        WHERE status = 'ACTIVE'
        ORDER BY CASE WHEN sortOrder IS NULL THEN 1 ELSE 0 END, sortOrder ASC, createdAt ASC
        """,
    )
    fun observeActive(): Flow<List<GoalEntity>>

    @Query(
        """
        SELECT * FROM goals
        WHERE status = 'ACTIVE'
        ORDER BY CASE WHEN sortOrder IS NULL THEN 1 ELSE 0 END, sortOrder ASC, createdAt ASC
        """,
    )
    suspend fun listActive(): List<GoalEntity>

    @Query(
        """
        SELECT * FROM goals
        ORDER BY CASE WHEN sortOrder IS NULL THEN 1 ELSE 0 END, sortOrder ASC, createdAt ASC
        """,
    )
    fun observeAll(): Flow<List<GoalEntity>>

    @Query(
        """
        SELECT * FROM goals
        ORDER BY CASE WHEN sortOrder IS NULL THEN 1 ELSE 0 END, sortOrder ASC, createdAt ASC
        """,
    )
    suspend fun listAll(): List<GoalEntity>

    @Query("SELECT * FROM goals WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): GoalEntity?

    @Query("SELECT * FROM goals WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<GoalEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(goal: GoalEntity)

    @Query("UPDATE goals SET status = 'ARCHIVED' WHERE id = :id")
    suspend fun archive(id: String)

    @Query("UPDATE goals SET status = 'COMPLETED' WHERE id = :id")
    suspend fun markCompleted(id: String)

    @Query(
        """
        SELECT COALESCE(SUM(allocationBps), 0) FROM goals
        WHERE status = 'ACTIVE' AND (:excludeId IS NULL OR id != :excludeId)
        """,
    )
    suspend fun sumActiveBpsExcluding(excludeId: String?): Int
}

@Dao
interface GoalAllocationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(allocation: GoalAllocationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(allocations: List<GoalAllocationEntity>)

    @Query("DELETE FROM goal_allocations WHERE sessionId = :sessionId")
    suspend fun deleteForSession(sessionId: String)

    @Query("DELETE FROM goal_allocations WHERE goalId = :goalId")
    suspend fun deleteForGoal(goalId: String)

    @Query("SELECT * FROM goal_allocations WHERE sessionId = :sessionId")
    suspend fun listForSession(sessionId: String): List<GoalAllocationEntity>

    @Query("SELECT * FROM goal_allocations WHERE goalId = :goalId ORDER BY createdAt ASC")
    suspend fun listForGoal(goalId: String): List<GoalAllocationEntity>

    @Query("SELECT * FROM goal_allocations")
    fun observeAll(): Flow<List<GoalAllocationEntity>>

    @Query("SELECT * FROM goal_allocations")
    suspend fun listAll(): List<GoalAllocationEntity>

    @Query("SELECT COALESCE(SUM(amountMinor), 0) FROM goal_allocations WHERE goalId = :goalId")
    suspend fun sumForGoal(goalId: String): Long

    @Query("SELECT COALESCE(SUM(amountMinor), 0) FROM goal_allocations WHERE goalId = :goalId")
    fun observeSumForGoal(goalId: String): Flow<Long>
}

@Dao
interface AchievementUnlockDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(unlock: AchievementUnlockEntity): Long

    @Query("SELECT * FROM achievement_unlocks ORDER BY unlockedAt ASC")
    fun observeAll(): Flow<List<AchievementUnlockEntity>>

    @Query("SELECT * FROM achievement_unlocks ORDER BY unlockedAt ASC")
    suspend fun listAll(): List<AchievementUnlockEntity>

    @Query("SELECT * FROM achievement_unlocks WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): AchievementUnlockEntity?

    @Query("SELECT id FROM achievement_unlocks")
    suspend fun listIds(): List<String>
}
