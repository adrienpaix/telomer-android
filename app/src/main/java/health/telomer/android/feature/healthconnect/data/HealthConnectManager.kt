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
            HealthPermission.getReadPermission(RestingHeartRateRecord::class),
            HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class),
            HealthPermission.getReadPermission(SleepSessionRecord::class),
            HealthPermission.getReadPermission(WeightRecord::class),
            HealthPermission.getReadPermission(BodyFatRecord::class),
            HealthPermission.getReadPermission(BodyWaterMassRecord::class),
            HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
            HealthPermission.getReadPermission(ExerciseSessionRecord::class),
            HealthPermission.getReadPermission(OxygenSaturationRecord::class),
            HealthPermission.getReadPermission(BodyTemperatureRecord::class),
        )

        // Default age for HR zone calculation (used when no user profile available)
        const val DEFAULT_AGE = 35
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

    // ── Time helpers ──────────────────────────────────────────────

    private fun timeRangeDays(days: Int): TimeRangeFilter {
        val now = Instant.now()
        val from = now.minus(Duration.ofDays(days.toLong()))
        return TimeRangeFilter.between(from, now)
    }

    private fun timeRangeToday(): TimeRangeFilter {
        val startOfDay = ZonedDateTime.now(ZoneId.systemDefault())
            .toLocalDate()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
        return TimeRangeFilter.between(startOfDay, Instant.now())
    }

    // ── Read data ─────────────────────────────────────────────────

    /** Read all metrics for a given time range. Returns a flat list ready for sync. */
    suspend fun readMetrics(
        from: Instant,
        to: Instant = Instant.now(),
        userAge: Int = DEFAULT_AGE,
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

        // Heart rate + zones
        val fcmax = (220 - userAge).toDouble()
        var z1 = 0L; var z2 = 0L; var z3 = 0L; var z4 = 0L; var z5 = 0L
        c.readRecords(ReadRecordsRequest(HeartRateRecord::class, timeRange)).records.forEach { r ->
            r.samples.forEach { s ->
                results += HealthMetric(
                    type = MetricType.HEART_RATE,
                    value = s.beatsPerMinute.toDouble(),
                    unit = MetricType.HEART_RATE.unit,
                    recordedAt = s.time,
                )
                val pct = s.beatsPerMinute.toDouble() / fcmax
                when {
                    pct < 0.60 -> z1++
                    pct < 0.70 -> z2++
                    pct < 0.80 -> z3++
                    pct < 0.90 -> z4++
                    else -> z5++
                }
            }
        }
        // Each sample ≈ 1 second; divide by 60 for minutes
        if (z1 + z2 + z3 + z4 + z5 > 0) {
            val recordedAt = to
            if (z1 > 0) results += HealthMetric(MetricType.HEART_ZONE_1, (z1 / 60.0), MetricType.HEART_ZONE_1.unit, recordedAt)
            if (z2 > 0) results += HealthMetric(MetricType.HEART_ZONE_2, (z2 / 60.0), MetricType.HEART_ZONE_2.unit, recordedAt)
            if (z3 > 0) results += HealthMetric(MetricType.HEART_ZONE_3, (z3 / 60.0), MetricType.HEART_ZONE_3.unit, recordedAt)
            if (z4 > 0) results += HealthMetric(MetricType.HEART_ZONE_4, (z4 / 60.0), MetricType.HEART_ZONE_4.unit, recordedAt)
            if (z5 > 0) results += HealthMetric(MetricType.HEART_ZONE_5, (z5 / 60.0), MetricType.HEART_ZONE_5.unit, recordedAt)
        }

        // Resting heart rate
        c.readRecords(ReadRecordsRequest(RestingHeartRateRecord::class, timeRange)).records.forEach { r ->
            results += HealthMetric(
                type = MetricType.RESTING_HEART_RATE,
                value = r.beatsPerMinute.toDouble(),
                unit = MetricType.RESTING_HEART_RATE.unit,
                recordedAt = r.time,
            )
        }

        // HRV
        c.readRecords(ReadRecordsRequest(HeartRateVariabilityRmssdRecord::class, timeRange)).records.forEach { r ->
            results += HealthMetric(
                type = MetricType.HRV,
                value = r.heartRateVariabilityMillis,
                unit = MetricType.HRV.unit,
                recordedAt = r.time,
            )
        }

        // Sleep (total duration + stages in metadata)
        c.readRecords(ReadRecordsRequest(SleepSessionRecord::class, timeRange)).records.forEach { r ->
            val totalMinutes = Duration.between(r.startTime, r.endTime).toMinutes().toDouble()
            var lightMin = 0L; var deepMin = 0L; var remMin = 0L
            r.stages.forEach { stage ->
                val mins = Duration.between(stage.startTime, stage.endTime).toMinutes()
                when (stage.stage) {
                    SleepSessionRecord.STAGE_TYPE_LIGHT -> lightMin += mins
                    SleepSessionRecord.STAGE_TYPE_DEEP -> deepMin += mins
                    SleepSessionRecord.STAGE_TYPE_REM -> remMin += mins
                    else -> {}
                }
            }
            results += HealthMetric(
                type = MetricType.SLEEP,
                value = totalMinutes,
                unit = MetricType.SLEEP.unit,
                recordedAt = r.endTime,
                metadata = mapOf(
                    "light_minutes" to lightMin.toDouble(),
                    "deep_minutes" to deepMin.toDouble(),
                    "rem_minutes" to remMin.toDouble(),
                    "bedtime_epoch" to r.startTime.epochSecond.toDouble(),
                    "wake_epoch" to r.endTime.epochSecond.toDouble(),
                ),
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

        // Body fat
        try {
            c.readRecords(ReadRecordsRequest(BodyFatRecord::class, timeRange)).records.forEach { r ->
                results += HealthMetric(
                    type = MetricType.BODY_FAT,
                    value = r.percentage.value,
                    unit = MetricType.BODY_FAT.unit,
                    recordedAt = r.time,
                )
            }
        } catch (_: Exception) { /* optional permission */ }

        // Body water mass
        try {
            c.readRecords(ReadRecordsRequest(BodyWaterMassRecord::class, timeRange)).records.forEach { r ->
                results += HealthMetric(
                    type = MetricType.BODY_WATER,
                    value = r.mass.inKilograms,
                    unit = MetricType.BODY_WATER.unit,
                    recordedAt = r.time,
                )
            }
        } catch (_: Exception) { /* optional permission */ }

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

        // SpO2
        try {
            c.readRecords(ReadRecordsRequest(OxygenSaturationRecord::class, timeRange)).records.forEach { r ->
                results += HealthMetric(
                    type = MetricType.SPO2,
                    value = r.percentage.value,
                    unit = MetricType.SPO2.unit,
                    recordedAt = r.time,
                )
            }
        } catch (_: Exception) { /* optional permission */ }

        // Body temperature
        try {
            c.readRecords(ReadRecordsRequest(BodyTemperatureRecord::class, timeRange)).records.forEach { r ->
                results += HealthMetric(
                    type = MetricType.BODY_TEMPERATURE,
                    value = r.temperature.inCelsius,
                    unit = MetricType.BODY_TEMPERATURE.unit,
                    recordedAt = r.time,
                )
            }
        } catch (_: Exception) { /* optional permission */ }

        return results
    }

    /** Convenience: read last N days of data. */
    suspend fun readLastDays(days: Int, userAge: Int = DEFAULT_AGE): List<HealthMetric> {
        val now = Instant.now()
        val from = now.minus(Duration.ofDays(days.toLong()))
        return readMetrics(from, now, userAge)
    }

    /** Read today's data (since midnight local time). */
    suspend fun readToday(userAge: Int = DEFAULT_AGE): List<HealthMetric> {
        val startOfDay = ZonedDateTime.now(ZoneId.systemDefault())
            .toLocalDate()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
        return readMetrics(startOfDay, Instant.now(), userAge)
    }
}
