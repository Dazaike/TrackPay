package com.trackpay.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.trackpay.app.data.local.entity.BreakIntervalEntity
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
}
