package health.telomer.android.feature.nutrition.data.repository

import health.telomer.android.feature.nutrition.data.api.AddMealItemRequest
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
        api.getDailySummary(date).toDomain()
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
        api.addMealItem(date, AddMealItemRequest(foodItemId, quantityG, mealType.name)).toDomain()
    }

    override suspend fun deleteMealItem(mealItemId: String): Result<Unit> = runCatching {
        api.deleteMealItem(mealItemId)
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
