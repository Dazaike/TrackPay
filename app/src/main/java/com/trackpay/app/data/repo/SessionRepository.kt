package com.trackpay.app.data.repo

import com.trackpay.app.data.local.dao.BreakIntervalDao
import com.trackpay.app.data.local.dao.JobDao
import com.trackpay.app.data.local.dao.WorkSessionDao
import com.trackpay.app.data.local.toDomain
import com.trackpay.app.data.local.toEntity
import com.trackpay.app.domain.model.ActiveSession
import com.trackpay.app.domain.model.BreakInterval
import com.trackpay.app.domain.model.WorkSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor(
    private val workSessionDao: WorkSessionDao,
    private val breakIntervalDao: BreakIntervalDao,
    private val jobDao: JobDao,
) {
    suspend fun getActiveSession(): WorkSession? =
        workSessionDao.getActive()?.toDomain()

    suspend fun getById(id: String): WorkSession? =
        workSessionDao.getById(id)?.toDomain()

    suspend fun insert(session: WorkSession) {
        workSessionDao.insert(session.toEntity())
    }

    suspend fun update(session: WorkSession) {
        workSessionDao.update(session.toEntity())
    }

    suspend fun listBreaks(sessionId: String): List<BreakInterval> =
        breakIntervalDao.listForSession(sessionId).map { it.toDomain() }

    suspend fun insertBreak(breakInterval: BreakInterval) {
        breakIntervalDao.insert(breakInterval.toEntity())
    }

    suspend fun updateBreak(breakInterval: BreakInterval) {
        breakIntervalDao.update(breakInterval.toEntity())
    }

    suspend fun getOpenBreak(sessionId: String): BreakInterval? =
        breakIntervalDao.getOpenBreak(sessionId)?.toDomain()

    fun observeCompletedSessions(): Flow<List<WorkSession>> =
        workSessionDao.observeCompleted().map { rows -> rows.map { it.toDomain() } }

    suspend fun listCompletedBetween(fromInclusive: Long, toExclusive: Long): List<WorkSession> =
        workSessionDao.listCompletedBetween(fromInclusive, toExclusive).map { it.toDomain() }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeActiveSession(): Flow<ActiveSession?> =
        workSessionDao.observeActive()
            .distinctUntilChanged()
            .flatMapLatest { entity ->
                if (entity == null) {
                    flowOf(null)
                } else {
                    combine(
                        jobDao.observeById(entity.jobId),
                        breakIntervalDao.observeForSession(entity.id),
                        workSessionDao.observeActive(),
                    ) { jobEntity, breaks, active ->
                        val sessionEntity = active ?: return@combine null
                        if (sessionEntity.id != entity.id) return@combine null
                        val job = jobEntity?.toDomain() ?: return@combine null
                        ActiveSession(
                            session = sessionEntity.toDomain(),
                            job = job,
                            breaks = breaks.map { it.toDomain() },
                        )
                    }
                }
            }
            .distinctUntilChanged()
}
