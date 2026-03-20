package health.telomer.android.core.data.api.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class HealthOSDashboardResponse(
    @Json(name = "patient_id") val patientId: String,
    @Json(name = "global_score") val globalScore: Double?,
    @Json(name = "global_confidence") val globalConfidence: String?,
    val inflammation: InflammationResponse?,
    val pillars: List<PillarSummaryResponse>,
    @Json(name = "computed_at") val computedAt: String?,
)

@JsonClass(generateAdapter = true)
data class InflammationResponse(
    val index: Int?,
    val level: String?,
)

@JsonClass(generateAdapter = true)
data class PillarSummaryResponse(
    val code: String,
    val label: String,
    val score: Int?,
    val confidence: String?,
    val trend: String?,
    @Json(name = "completeness_pct") val completenessPct: Int?,
)

@JsonClass(generateAdapter = true)
data class PillarDetailResponse(
    val code: String,
    val label: String,
    val score: Int?,
    val confidence: String?,
    @Json(name = "completeness_pct") val completenessPct: Int?,
    val metrics: List<MetricResponse>,
    @Json(name = "missing_metrics") val missingMetrics: List<String>,
)

@JsonClass(generateAdapter = true)
data class MetricResponse(
    val code: String,
    val label: String,
    val value: Double?,
    val unit: String?,
    val status: String?,
    @Json(name = "scored_at") val scoredAt: String?,
)

@JsonClass(generateAdapter = true)
data class BiomarkersLatestResponse(
    @Json(name = "patient_id") val patientId: String?,
    val biomarkers: List<BiomarkerLatestResponse>,
)

@JsonClass(generateAdapter = true)
data class BiomarkerLatestResponse(
    val code: String,
    val label: String,
    val value: Double?,
    val unit: String?,
    val status: String?,
    @Json(name = "pillar_code") val pillarCode: String?,
    @Json(name = "scored_at") val scoredAt: String?,
)
