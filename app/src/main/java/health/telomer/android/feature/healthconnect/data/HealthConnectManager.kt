package health.telomer.android.feature.healthconnect.data

import android.content.Context
import android.os.Build
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import health.telomer.android.feature.healthconnect.domain.HealthMetric
import health.telomer.android.feature.healthconnect.domain.MetricType
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/** Availability status of Health Connect on the device. */
enum class HealthConnectAvailability {
    AVAILABLE,
    NOT_INSTALLED,
    NOT_SUPPORTED,
}

@Singleton
class HealthConnectManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        val PERMISSIONS = setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getReadPermission(HeartRateRecord::class),
            HealthPermission.getReadPermission(SleepSessionRecord::class),
            HealthPermission.getReadPermission(WeightRecord::class),
            HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
            HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        )
    }

    private var _client: HealthConnectClient? = null
    val client: HealthConnectClient?
        get() = _client

    // ── Availability ──────────────────────────────────────────────

    fun checkAvailability(): HealthConnectAvailability {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return HealthConnectAvailability.NOT_SUPPORTED
        }
        val status = HealthConnectClient.getSdkStatus(context)
        return when (status) {
            HealthConnectClient.SDK_AVAILABLE -> {
                _client = HealthConnectClient.getOrCreate(context)
                HealthConnectAvailability.AVAILABLE
            }
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED ->
                HealthConnectAvailability.NOT_INSTALLED
            else -> HealthConnectAvailability.NOT_SUPPORTED
        }
    }

    // ── Permissions ───────────────────────────────────────────────

    suspend fun hasAllPermissions(): Boolean {
        val c = _client ?: return false
        val granted = c.permissionController.getGrantedPermissions()
        return PERMISSIONS.all { it in granted }
    }

    fun permissionContract() = PermissionController.createRequestPermissionResultContract()

    // ── Read data ─────────────────────────────────────────────────

    /** Read metrics for a given time range. Returns a flat list ready for sync. */
    suspend fun readMetrics(
        from: Instant,
        to: Instant = Instant.now(),
    ): List<HealthMetric> {
        val c = _client ?: return emptyList()
        val timeRange = TimeRangeFilter.between(from, to)
        val results = mutableListOf<HealthMetric>()

        // Steps
        c.readRecords(ReadRecordsRequest(StepsRecord::class, timeRange)).records.forEach { r ->
            results += HealthMetric(
                type = MetricType.STEPS,
                value = r.count.toDouble(),
                unit = MetricType.STEPS.unit,
                recordedAt = r.endTime,
            )
        }

        // Heart rate
        c.readRecords(ReadRecordsRequest(HeartRateRecord::class, timeRange)).records.forEach { r ->
            r.samples.forEach { s ->
                results += HealthMetric(
                    type = MetricType.HEART_RATE,
                    value = s.beatsPerMinute.toDouble(),
                    unit = MetricType.HEART_RATE.unit,
                    recordedAt = s.time,
                )
            }
        }

        // Sleep
        c.readRecords(ReadRecordsRequest(SleepSessionRecord::class, timeRange)).records.forEach { r ->
            val minutes = Duration.between(r.startTime, r.endTime).toMinutes().toDouble()
            results += HealthMetric(
                type = MetricType.SLEEP,
                value = minutes,
                unit = MetricType.SLEEP.unit,
                recordedAt = r.endTime,
            )
        }

        // Weight
        c.readRecords(ReadRecordsRequest(WeightRecord::class, timeRange)).records.forEach { r ->
            results += HealthMetric(
                type = MetricType.WEIGHT,
                value = r.weight.inKilograms,
                unit = MetricType.WEIGHT.unit,
                recordedAt = r.time,
            )
        }

        // Active calories
        c.readRecords(ReadRecordsRequest(ActiveCaloriesBurnedRecord::class, timeRange)).records.forEach { r ->
            results += HealthMetric(
                type = MetricType.ACTIVE_CALORIES,
                value = r.energy.inKilocalories,
                unit = MetricType.ACTIVE_CALORIES.unit,
                recordedAt = r.endTime,
            )
        }

        // Exercise sessions
        c.readRecords(ReadRecordsRequest(ExerciseSessionRecord::class, timeRange)).records.forEach { r ->
            val minutes = Duration.between(r.startTime, r.endTime).toMinutes().toDouble()
            results += HealthMetric(
                type = MetricType.EXERCISE,
                value = minutes,
                unit = MetricType.EXERCISE.unit,
                recordedAt = r.endTime,
            )
        }

        return results
    }

    /** Convenience: read last N days of data. */
    suspend fun readLastDays(days: Int): List<HealthMetric> {
        val now = Instant.now()
        val from = now.minus(Duration.ofDays(days.toLong()))
        return readMetrics(from, now)
    }

    /** Read today's data (since midnight local time). */
    suspend fun readToday(): List<HealthMetric> {
        val startOfDay = ZonedDateTime.now(ZoneId.systemDefault())
            .toLocalDate()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
        return readMetrics(startOfDay)
    }
}
