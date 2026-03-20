package health.telomer.android.feature.nutrition.data.mapper

import health.telomer.android.feature.nutrition.data.api.*
import health.telomer.android.feature.nutrition.domain.model.*

// ── Food database item (search) ────────────────────────
fun FoodItemDto.toDomain() = FoodItem(
    id = id,
    name = name,
    nameFr = nameFr,
    barcode = barcode,
    caloriesKcal = caloriesKcal,
    proteinsG = proteinsG,
    carbsG = carbsG,
    fatsG = fatsG,
    fiberG = fiberG,
    imageUrl = imageUrl,
)

// ── Food log item (backend list response) ─────────────
fun FoodLogItemDto.toDomain() = FoodItem(
    id = id,
    name = foodName,
    nameFr = null,
    barcode = barcode,
    caloriesKcal = calories,
    proteinsG = proteinG,
    carbsG = carbsG,
    fatsG = fatG,
    fiberG = fiberG,
    imageUrl = null,
)

fun FoodLogItemDto.toMealLogItem() = MealLogItem(
    id = id,
    foodItem = this.toDomain(),
    quantityG = quantityG ?: 100.0,
    caloriesKcal = calories,
    proteinsG = proteinG,
    carbsG = carbsG,
    fatsG = fatG,
)

// ── Goals ──────────────────────────────────────────────
fun NutritionGoalDto.toDomain() = NutritionGoal(
    caloriesKcal = caloriesKcal,
    proteinsG = proteinsG,
    carbsG = carbsG,
    fatsG = fatsG,
)

fun NutritionGoal.toDto() = NutritionGoalDto(
    caloriesKcal = caloriesKcal,
    proteinsG = proteinsG,
    carbsG = carbsG,
    fatsG = fatsG,
)
