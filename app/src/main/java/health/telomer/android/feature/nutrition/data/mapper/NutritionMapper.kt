package health.telomer.android.feature.nutrition.data.mapper

import health.telomer.android.feature.nutrition.data.api.*
import health.telomer.android.feature.nutrition.domain.model.*

fun FoodItemDto.toDomain() = FoodItem(
    id = id, name = name, nameFr = nameFr, barcode = barcode,
    caloriesKcal = caloriesKcal, proteinsG = proteinsG, carbsG = carbsG,
    fatsG = fatsG, fiberG = fiberG, imageUrl = imageUrl,
)

fun MealLogItemDto.toDomain() = MealLogItem(
    id = id, foodItem = foodItem.toDomain(), quantityG = quantityG,
    caloriesKcal = caloriesKcal, proteinsG = proteinsG, carbsG = carbsG, fatsG = fatsG,
)

fun MealLogDto.toDomain() = MealLog(
    id = id, date = date,
    mealType = when (mealType.uppercase()) {
        "BREAKFAST" -> MealType.BREAKFAST
        "LUNCH" -> MealType.LUNCH
        "DINNER" -> MealType.DINNER
        else -> MealType.SNACK
    },
    items = items.map { it.toDomain() },
    notes = notes,
)

fun NutritionGoalDto.toDomain() = NutritionGoal(
    caloriesKcal = caloriesKcal, proteinsG = proteinsG, carbsG = carbsG, fatsG = fatsG,
)

fun NutritionGoal.toDto() = NutritionGoalDto(
    caloriesKcal = caloriesKcal, proteinsG = proteinsG, carbsG = carbsG, fatsG = fatsG,
)

fun DailySummaryDto.toDomain() = DailySummary(
    date = date, totalCalories = totalCalories, totalProteins = totalProteins,
    totalCarbs = totalCarbs, totalFats = totalFats,
    meals = meals.map { it.toDomain() }, goal = goal?.toDomain(),
)
