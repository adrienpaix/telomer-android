package health.telomer.android.feature.nutrition.domain.repository

import health.telomer.android.feature.nutrition.domain.model.*

interface NutritionRepository {
    suspend fun getDailySummary(date: String): Result<DailySummary>
    suspend fun searchFood(query: String): Result<List<FoodItem>>
    suspend fun getFoodByBarcode(ean: String): Result<FoodItem>
    suspend fun addMealItem(date: String, mealType: MealType, foodItemId: String, quantityG: Double): Result<MealLogItem>
    suspend fun deleteMealItem(mealItemId: String): Result<Unit>
    suspend fun deleteMeal(mealId: String): Result<Unit>
    suspend fun getGoals(): Result<NutritionGoal>
    suspend fun updateGoals(goal: NutritionGoal): Result<NutritionGoal>
    suspend fun createDirectFoodLog(
        foodName: String,
        calories: Double?,
        proteinG: Double?,
        carbsG: Double?,
        fatG: Double?,
        source: String,
    ): Result<Unit>
}
