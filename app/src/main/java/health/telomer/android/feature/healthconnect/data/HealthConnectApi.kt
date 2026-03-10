package health.telomer.android.feature.healthconnect.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.*

interface HealthConnectApi {

    @POST("me/health/metrics/sync")
    suspend fun syncMetrics(@Body body: MetricsSyncRequest): MetricsSyncResponse

    @GET("me/health/metrics")
    suspend fun getMetrics(
        @Query("type") type: String,
        @Query("from") from: String,
        @Query("to") to: String,
    ): List<HealthMetricResponse>

    @GET("me/health/metrics/summary")
    suspend fun getDaySummary(@Query("date") date: String): Map<String, Any>
}

@JsonClass(generateAdapter = true)
data class MetricsSyncRequest(
    @Json(name = "metrics") val metrics: List<MetricPayload>,
)

@JsonClass(generateAdapter = true)
data class MetricPayload(
    @Json(name = "type") val type: String,
    @Json(name = "value") val value: Double,
    @Json(name = "unit") val unit: String,
    @Json(name = "recorded_at") val recordedAt: String,
)

@JsonClass(generateAdapter = true)
data class MetricsSyncResponse(
    @Json(name = "synced") val synced: Int = 0,
    @Json(name = "duplicates") val duplicates: Int = 0,
)

@JsonClass(generateAdapter = true)
data class HealthMetricResponse(
    @Json(name = "type") val type: String,
    @Json(name = "value") val value: Double,
    @Json(name = "unit") val unit: String,
    @Json(name = "recorded_at") val recordedAt: String,
)
