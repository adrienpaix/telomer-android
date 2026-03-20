package health.telomer.android.feature.nutrition.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.*

// ── Search DTOs (food database) ─────────────────────────
@JsonClass(generateAdapter = true)
data class FoodItemDto(
    val id: String,
    val name: String,
    @Json(name = "name_fr") val nameFr: String? = null,
    val barcode: String? = null,
    @Json(name = "calories_kcal") val caloriesKcal: Double? = null,
    @Json(name = "proteins_g") val proteinsG: Double? = null,
    @Json(name = "carbs_g") val carbsG: Double? = null,
    @Json(name = "fats_g") val fatsG: Double? = null,
    @Json(name = "fiber_g") val fiberG: Double? = null,
    @Json(name = "image_url") val imageUrl: String? = null,
)

@JsonClass(generateAdapter = true)
data class FoodSearchResponse(
    val items: List<FoodItemDto> = emptyList(),
    val count: Int = 0,
)

// ── Food Log DTOs (aligned with real backend) ──────────
@JsonClass(generateAdapter = true)
data class FoodLogItemDto(
    val id: String,
    @Json(name = "food_name") val foodName: String,
    @Json(name = "meal_type") val mealType: String? = null,
    @Json(name = "quantity_g") val quantityG: Double? = null,
    val calories: Double? = null,
    @Json(name = "protein_g") val proteinG: Double? = null,
    @Json(name = "carbs_g") val carbsG: Double? = null,
    @Json(name = "fat_g") val fatG: Double? = null,
    @Json(name = "fiber_g") val fiberG: Double? = null,
    val source: String? = null,
    @Json(name = "logged_at") val loggedAt: String? = null,
    val confidence: Double? = null,
    val barcode: String? = null,
)

@JsonClass(generateAdapter = true)
data class FoodLogSummaryDto(
    val date: String,
    @Json(name = "total_calories") val totalCalories: Double = 0.0,
    @Json(name = "total_protein_g") val totalProteinG: Double = 0.0,
    @Json(name = "total_carbs_g") val totalCarbsG: Double = 0.0,
    @Json(name = "total_fat_g") val totalFatG: Double = 0.0,
    @Json(name = "total_fiber_g") val totalFiberG: Double = 0.0,
    @Json(name = "meal_count") val mealCount: Int = 0,
)

@JsonClass(generateAdapter = true)
data class FoodLogCreateRequest(
    @Json(name = "food_name") val foodName: String,
    @Json(name = "meal_type") val mealType: String? = null,
    @Json(name = "quantity_g") val quantityG: Double? = 100.0,
    val calories: Double? = null,
    @Json(name = "protein_g") val proteinG: Double? = null,
    @Json(name = "carbs_g") val carbsG: Double? = null,
    @Json(name = "fat_g") val fatG: Double? = null,
    @Json(name = "fiber_g") val fiberG: Double? = null,
    val source: String? = "manual",
    val barcode: String? = null,
)

@JsonClass(generateAdapter = true)
data class NutritionGoalDto(
    @Json(name = "calories_kcal") val caloriesKcal: Int? = null,
    @Json(name = "proteins_g") val proteinsG: Int? = null,
    @Json(name = "carbs_g") val carbsG: Int? = null,
    @Json(name = "fats_g") val fatsG: Int? = null,
)

@JsonClass(generateAdapter = true)
data class AnalyzeTextRequest(val text: String)

@JsonClass(generateAdapter = true)
data class AnalyzeTextResponseDto(
    @Json(name = "food_name") val foodName: String? = null,
    val calories: Double? = null,
    @Json(name = "protein_g") val proteinG: Double? = null,
    @Json(name = "carbs_g") val carbsG: Double? = null,
    @Json(name = "fat_g") val fatG: Double? = null,
    @Json(name = "fiber_g") val fiberG: Double? = null,
    val quantity: Double? = null,
    val unit: String? = null,
    val confidence: Double? = null,
)

@JsonClass(generateAdapter = true)
data class AnalyzeBarcodeRequest(val barcode: String)

@JsonClass(generateAdapter = true)
data class AnalyzePhotoRequest(
    @Json(name = "image_base64") val imageBase64: String,
    @Json(name = "mime_type") val mimeType: String = "image/jpeg",
)

@JsonClass(generateAdapter = true)
data class AnalyzePhotoResponseDto(
    @Json(name = "food_name") val foodName: String? = null,
    val calories: Double? = null,
    @Json(name = "protein_g") val proteinG: Double? = null,
    @Json(name = "carbs_g") val carbsG: Double? = null,
    @Json(name = "fat_g") val fatG: Double? = null,
    @Json(name = "fiber_g") val fiberG: Double? = null,
    @Json(name = "quantity_g") val quantityG: Double? = null,
    val confidence: Double? = null,
)

// ── Retrofit Interface ──────────────────────────────────
interface NutritionApi {

    @GET("me/food-log")
    suspend fun getFoodLogs(@Query("date") date: String? = null): List<FoodLogItemDto>

    @POST("me/food-log")
    suspend fun createFoodLog(@Body request: FoodLogCreateRequest): FoodLogItemDto

    @DELETE("me/food-log/{id}")
    suspend fun deleteFoodLog(@Path("id") id: String)

    @GET("me/food-log/summary")
    suspend fun getSummary(@Query("date") date: String? = null): FoodLogSummaryDto

    @POST("me/food-log/analyze/text")
    suspend fun analyzeText(@Body request: AnalyzeTextRequest): AnalyzeTextResponseDto

    @POST("me/food-log/analyze/photo")
    suspend fun analyzePhoto(@Body request: AnalyzePhotoRequest): AnalyzePhotoResponseDto

    @POST("me/food-log/analyze/barcode")
    suspend fun analyzeBarcode(@Body request: AnalyzeBarcodeRequest): FoodLogItemDto

    @GET("me/food-log/food/search")
    suspend fun searchFood(@Query("q") query: String): FoodSearchResponse

    @GET("me/food-log/food/barcode/{ean}")
    suspend fun getFoodByBarcode(@Path("ean") ean: String): FoodItemDto

    @GET("me/nutrition/goals")
    suspend fun getGoals(): NutritionGoalDto

    @PUT("me/nutrition/goals")
    suspend fun updateGoals(@Body goal: NutritionGoalDto): NutritionGoalDto
}
