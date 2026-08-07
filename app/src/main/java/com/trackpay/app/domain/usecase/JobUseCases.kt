package com.trackpay.app.domain.usecase

import com.trackpay.app.data.repo.JobRepository
import com.trackpay.app.domain.model.Job
import com.trackpay.app.domain.model.JobDefaults
import com.trackpay.app.domain.time.Clock
import com.trackpay.app.location.GeofenceManager
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpsertJobUseCase @Inject constructor(
    private val jobRepository: JobRepository,
    private val clock: Clock,
    private val geofenceManager: GeofenceManager,
) {
    suspend operator fun invoke(
        id: String? = null,
        name: String,
        hourlyRateMinor: Long,
        otRateMinor: Long? = null,
        otThresholdMinutes: Int? = null,
        colorArgb: Int = JobDefaults.DEFAULT_COLOR_ARGB,
        iconKey: String = JobDefaults.DEFAULT_ICON_KEY,
        geoEnabled: Boolean? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        radiusMeters: Int? = null,
        clearGeo: Boolean = false,
    ): Job {
        require(name.isNotBlank()) { "Job name is required" }
        require(hourlyRateMinor > 0L) { "Hourly rate must be positive" }
        if (otRateMinor != null) {
            require(otRateMinor > 0L) { "OT rate must be positive when set" }
        }
        val existing = id?.let { jobRepository.getById(it) }
        val resolvedGeoEnabled = when {
            clearGeo -> false
            geoEnabled != null -> geoEnabled
            else -> existing?.geoEnabled ?: false
        }
        val resolvedLat = when {
            clearGeo -> null
            latitude != null -> latitude
            geoEnabled == false -> null
            else -> existing?.latitude
        }
        val resolvedLng = when {
            clearGeo -> null
            longitude != null -> longitude
            geoEnabled == false -> null
            else -> existing?.longitude
        }
        val resolvedRadius = when {
            clearGeo -> null
            radiusMeters != null -> radiusMeters
            geoEnabled == false -> null
            resolvedGeoEnabled && existing?.radiusMeters == null &&
                (resolvedLat != null && resolvedLng != null) -> JobDefaults.DEFAULT_RADIUS_METERS
            else -> existing?.radiusMeters
        }
        if (resolvedGeoEnabled) {
            require(resolvedLat != null && resolvedLng != null) {
                "Latitude and longitude are required when geo is enabled"
            }
            require(resolvedRadius != null && resolvedRadius > 0) {
                "Radius must be positive when geo is enabled"
            }
        }
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
            geoEnabled = resolvedGeoEnabled,
            latitude = resolvedLat,
            longitude = resolvedLng,
            radiusMeters = resolvedRadius,
        )
        jobRepository.upsert(job)
        runCatching { geofenceManager.refresh() }
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
    private val geofenceManager: GeofenceManager,
) {
    suspend operator fun invoke(jobId: String) {
        jobRepository.archive(jobId)
        runCatching { geofenceManager.refresh() }
    }
}

@Singleton
class GetJobUseCase @Inject constructor(
    private val jobRepository: JobRepository,
) {
    suspend operator fun invoke(jobId: String): Job? = jobRepository.getById(jobId)
}
