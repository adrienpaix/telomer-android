package health.telomer.android.feature.nutrition.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.*

// ── DTOs ────────────────────────────────────────────────

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
data class MealLogItemDto(
    val id: String,
    @Json(name = "food_item") val foodItem: FoodItemDto,
    @Json(name = "quantity_g") val quantityG: Double,
    @Json(name = "calories_kcal") val caloriesKcal: Double? = null,
    @Json(name = "proteins_g") val proteinsG: Double? = null,
    @Json(name = "carbs_g") val carbsG: Double? = null,
    @Json(name = "fats_g") val fatsG: Double? = null,
)

@JsonClass(generateAdapter = true)
data class MealLogDto(
    val id: String,
    val date: String,
    @Json(name = "meal_type") val mealType: String,
    val items: List<MealLogItemDto>,
    val notes: String? = null,
)

@JsonClass(generateAdapter = true)
data class NutritionGoalDto(
    @Json(name = "calories_kcal") val caloriesKcal: Int? = null,
    @Json(name = "proteins_g") val proteinsG: Int? = null,
    @Json(name = "carbs_g") val carbsG: Int? = null,
    @Json(name = "fats_g") val fatsG: Int? = null,
)

@JsonClass(generateAdapter = true)
data class DayTotalsDto(
    @Json(name = "calories_kcal") val caloriesKcal: Double = 0.0,
    @Json(name = "proteins_g") val proteinsG: Double = 0.0,
    @Json(name = "carbs_g") val carbsG: Double = 0.0,
    @Json(name = "fats_g") val fatsG: Double = 0.0,
)

@JsonClass(generateAdapter = true)
data class MealLogDayResponseDto(
    val date: String,
    val meals: List<MealLogDto>,
    val totals: DayTotalsDto? = null,
)

@JsonClass(generateAdapter = true)
data class DailySummaryDto(
    val date: String,
    @Json(name = "total_calories") val totalCalories: Double,
    @Json(name = "total_proteins") val totalProteins: Double,
    @Json(name = "total_carbs") val totalCarbs: Double,
    @Json(name = "total_fats") val totalFats: Double,
    val meals: List<MealLogDto>,
    val goal: NutritionGoalDto? = null,
)

@JsonClass(generateAdapter = true)
data class AddMealItemRequest(
    @Json(name = "food_item_id") val foodItemId: String,
    @Json(name = "quantity_g") val quantityG: Double,
    @Json(name = "meal_type") val mealType: String,
)

@JsonClass(generateAdapter = true)
data class CreateMealRequest(
    val date: String,
    @Json(name = "meal_type") val mealType: String,
    val items: List<MealItemInput>? = null,
    val notes: String? = null,
)

@JsonClass(generateAdapter = true)
data class MealItemInput(
    @Json(name = "food_item_id") val foodItemId: String,
    @Json(name = "quantity_g") val quantityG: Double,
)

@JsonClass(generateAdapter = true)
data class FoodSearchResponse(
    val items: List<FoodItemDto> = emptyList(),
    val count: Int = 0,
)

@JsonClass(generateAdapter = true)
data class NutritionSummaryDto(
    val date: String,
    @Json(name = "total_calories") val totalCalories: Double = 0.0,
    @Json(name = "total_proteins") val totalProteins: Double = 0.0,
    @Json(name = "total_carbs") val totalCarbs: Double = 0.0,
    @Json(name = "total_fats") val totalFats: Double = 0.0,
    @Json(name = "meal_count") val mealCount: Int = 0,
    val goal: NutritionGoalDto? = null,
)

// ── Retrofit Interface ──────────────────────────────────

interface NutritionApi {

    @GET("me/nutrition/meals")
    suspend fun getMeals(@Query("date") date: String): MealLogDayResponseDto

    @POST("me/nutrition/meals")
    suspend fun createMeal(@Body request: CreateMealRequest): MealLogDto

    @PUT("me/nutrition/meals/{mealId}")
    suspend fun updateMeal(@Path("mealId") mealId: String, @Body request: CreateMealRequest): MealLogDto

    @DELETE("me/nutrition/meals/{mealId}")
    suspend fun deleteMeal(@Path("mealId") mealId: String)

    @GET("me/nutrition/summary")
    suspend fun getSummary(@Query("date") date: String): NutritionSummaryDto

    @GET("me/nutrition/food/search")
    suspend fun searchFood(@Query("q") query: String): FoodSearchResponse

    @GET("me/nutrition/food/barcode/{ean}")
    suspend fun getFoodByBarcode(@Path("ean") ean: String): FoodItemDto

    @GET("me/nutrition/goals")
    suspend fun getGoals(): NutritionGoalDto

    @PUT("me/nutrition/goals")
    suspend fun updateGoals(@Body goal: NutritionGoalDto): NutritionGoalDto
}
