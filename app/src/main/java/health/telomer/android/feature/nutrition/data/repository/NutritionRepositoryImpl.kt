package health.telomer.android.feature.nutrition.data.repository

import health.telomer.android.feature.nutrition.data.api.CreateMealRequest
import health.telomer.android.feature.nutrition.data.api.MealItemInput
import health.telomer.android.feature.nutrition.data.api.NutritionApi
import health.telomer.android.feature.nutrition.data.mapper.*
import health.telomer.android.feature.nutrition.domain.model.*
import health.telomer.android.feature.nutrition.domain.repository.NutritionRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NutritionRepositoryImpl @Inject constructor(
    private val api: NutritionApi,
) : NutritionRepository {

    override suspend fun getDailySummary(date: String): Result<DailySummary> = runCatching {
        val response = api.getMeals(date)
        DailySummary(
            date = response.date,
            totalCalories = response.totals?.caloriesKcal ?: 0.0,
            totalProteins = response.totals?.proteinsG ?: 0.0,
            totalCarbs = response.totals?.carbsG ?: 0.0,
            totalFats = response.totals?.fatsG ?: 0.0,
            meals = response.meals.map { it.toDomain() },
            goal = null,
        )
    }

    override suspend fun searchFood(query: String): Result<List<FoodItem>> = runCatching {
        api.searchFood(query).results.map { it.toDomain() }
    }

    override suspend fun getFoodByBarcode(ean: String): Result<FoodItem> = runCatching {
        api.getFoodByBarcode(ean).toDomain()
    }

    override suspend fun addMealItem(
        date: String, mealType: MealType, foodItemId: String, quantityG: Double,
    ): Result<MealLogItem> = runCatching {
        val meal = api.createMeal(
            CreateMealRequest(
                date = date,
                mealType = mealType.name,
                items = listOf(MealItemInput(foodItemId = foodItemId, quantityG = quantityG)),
            )
        )
        // Return the first item from the created meal
        meal.items.first().toDomain()
    }

    override suspend fun deleteMealItem(mealItemId: String): Result<Unit> = runCatching {
        // The backend doesn't have a delete-item endpoint, delete the whole meal
        api.deleteMeal(mealItemId)
    }

    override suspend fun deleteMeal(mealId: String): Result<Unit> = runCatching {
        api.deleteMeal(mealId)
    }

    override suspend fun getGoals(): Result<NutritionGoal> = runCatching {
        api.getGoals().toDomain()
    }

    override suspend fun updateGoals(goal: NutritionGoal): Result<NutritionGoal> = runCatching {
        api.updateGoals(goal.toDto()).toDomain()
    }
}
