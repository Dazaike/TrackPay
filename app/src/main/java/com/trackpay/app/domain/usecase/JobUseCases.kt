package com.trackpay.app.domain.usecase

import com.trackpay.app.data.repo.JobRepository
import com.trackpay.app.domain.model.Job
import com.trackpay.app.domain.model.JobDefaults
import com.trackpay.app.domain.time.Clock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpsertJobUseCase @Inject constructor(
    private val jobRepository: JobRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(
        id: String? = null,
        name: String,
        hourlyRateMinor: Long,
        otRateMinor: Long? = null,
        otThresholdMinutes: Int? = null,
        colorArgb: Int = JobDefaults.DEFAULT_COLOR_ARGB,
        iconKey: String = JobDefaults.DEFAULT_ICON_KEY,
    ): Job {
        require(name.isNotBlank()) { "Job name is required" }
        require(hourlyRateMinor > 0L) { "Hourly rate must be positive" }
        if (otRateMinor != null) {
            require(otRateMinor > 0L) { "OT rate must be positive when set" }
        }
        val existing = id?.let { jobRepository.getById(it) }
        val job = Job(
            id = existing?.id ?: id ?: UUID.randomUUID().toString(),
            name = name.trim(),
            hourlyRateMinor = hourlyRateMinor,
            otRateMinor = otRateMinor,
            otThresholdMinutes = when {
                otRateMinor == null -> null
                else -> otThresholdMinutes ?: JobDefaults.DEFAULT_OT_THRESHOLD_MINUTES
            },
            colorArgb = colorArgb,
            iconKey = iconKey,
            archived = existing?.archived ?: false,
            createdAt = existing?.createdAt ?: clock.now(),
        )
        jobRepository.upsert(job)
        return job
    }
}

@Singleton
class ListJobsUseCase @Inject constructor(
    private val jobRepository: JobRepository,
) {
    operator fun invoke() = jobRepository.observeActiveJobs()

    suspend fun once() = jobRepository.listActiveJobs()
}

@Singleton
class ArchiveJobUseCase @Inject constructor(
    private val jobRepository: JobRepository,
) {
    suspend operator fun invoke(jobId: String) {
        jobRepository.archive(jobId)
    }
}

@Singleton
class GetJobUseCase @Inject constructor(
    private val jobRepository: JobRepository,
) {
    suspend operator fun invoke(jobId: String): Job? = jobRepository.getById(jobId)
}
