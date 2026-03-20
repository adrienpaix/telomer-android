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
    @Json(name = "biological_age") val biologicalAge: Double? = null,
    @Json(name = "biological_age_source") val biologicalAgeSource: String? = null,
    @Json(name = "biological_age_missing") val biologicalAgeMissing: List<String>? = null,
    @Json(name = "chronological_age") val chronologicalAge: Int? = null,
    @Json(name = "adherence_pct") val adherencePct: Double? = null,
)

@JsonClass(generateAdapter = true)
data class InflammationResponse(
    val index: Double?,
    val level: String?,
    @Json(name = "pillar_impact") val pillarImpact: Map<String, Double>? = null,
)

@JsonClass(generateAdapter = true)
data class PillarSummaryResponse(
    val code: String,
    val label: String,
    val score: Double?,
    val confidence: String?,
    val trend: String?,
    @Json(name = "trend_delta") val trendDelta: Double? = null,
    @Json(name = "completeness_pct") val completenessPct: Double?,
    @Json(name = "alert_count") val alertCount: Int? = null,
    @Json(name = "last_updated") val lastUpdated: String? = null,
    @Json(name = "pathology_flags") val pathologyFlags: List<String>? = null,
)

@JsonClass(generateAdapter = true)
data class PillarDetailResponse(
    val code: String,
    val label: String,
    val score: Double?,
    val confidence: String?,
    @Json(name = "completeness_pct") val completenessPct: Double?,
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
