package com.trackpay.app.data.repo

import com.trackpay.app.data.local.dao.JobDao
import com.trackpay.app.data.local.toDomain
import com.trackpay.app.data.local.toEntity
import com.trackpay.app.domain.model.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JobRepository @Inject constructor(
    private val jobDao: JobDao,
) {
    fun observeActiveJobs(): Flow<List<Job>> =
        jobDao.observeActiveJobs().map { rows -> rows.map { it.toDomain() } }

    suspend fun listActiveJobs(): List<Job> =
        jobDao.listActiveJobs().map { it.toDomain() }

    suspend fun getById(id: String): Job? =
        jobDao.getById(id)?.toDomain()

    fun observeById(id: String): Flow<Job?> =
        jobDao.observeById(id).map { it?.toDomain() }

    suspend fun upsert(job: Job) {
        jobDao.upsert(job.toEntity())
    }

    suspend fun archive(id: String) {
        jobDao.archive(id)
    }
}
