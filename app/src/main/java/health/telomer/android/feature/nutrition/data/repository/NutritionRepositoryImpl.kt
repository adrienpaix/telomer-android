package health.telomer.android.feature.nutrition.data.repository

import health.telomer.android.feature.nutrition.data.api.*
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
        val items = api.getFoodLogs(date)
        val summary = runCatching { api.getSummary(date) }.getOrNull()

        // Group items by meal_type to build MealLog list
        val mealsByType = items.groupBy { it.mealType ?: "snack" }
        val meals = mealsByType.map { (type, logItems) ->
            MealLog(
                id = logItems.first().id,
                date = date,
                mealType = when (type.uppercase()) {
                    "BREAKFAST" -> MealType.BREAKFAST
                    "LUNCH" -> MealType.LUNCH
                    "DINNER" -> MealType.DINNER
                    else -> MealType.SNACK
                },
                items = logItems.map { it.toMealLogItem() },
                notes = null,
            )
        }

        DailySummary(
            date = summary?.date ?: date,
            totalCalories = summary?.totalCalories ?: items.sumOf { it.calories ?: 0.0 },
            totalProteins = summary?.totalProteinG ?: items.sumOf { it.proteinG ?: 0.0 },
            totalCarbs = summary?.totalCarbsG ?: items.sumOf { it.carbsG ?: 0.0 },
            totalFats = summary?.totalFatG ?: items.sumOf { it.fatG ?: 0.0 },
            meals = meals,
            goal = null,
        )
    }

    override suspend fun searchFood(query: String): Result<List<FoodItem>> = runCatching {
        api.searchFood(query).items.map { it.toDomain() }
    }

    override suspend fun getFoodByBarcode(ean: String): Result<FoodItem> = runCatching {
        api.getFoodByBarcode(ean).toDomain()
    }

    override suspend fun addMealItem(
        date: String, mealType: MealType, foodItemId: String, quantityG: Double,
    ): Result<MealLogItem> = runCatching {
        val item = api.createFoodLog(
            FoodLogCreateRequest(
                foodName = foodItemId,
                mealType = mealType.name,
                quantityG = quantityG,
                source = "manual",
            )
        )
        item.toMealLogItem()
    }

    override suspend fun deleteMealItem(mealItemId: String): Result<Unit> = runCatching {
        api.deleteFoodLog(mealItemId)
    }

    override suspend fun deleteMeal(mealId: String): Result<Unit> = runCatching {
        api.deleteFoodLog(mealId)
    }

    override suspend fun getGoals(): Result<NutritionGoal> = runCatching {
        api.getGoals().toDomain()
    }

    override suspend fun updateGoals(goal: NutritionGoal): Result<NutritionGoal> = runCatching {
        api.updateGoals(goal.toDto()).toDomain()
    }
}
