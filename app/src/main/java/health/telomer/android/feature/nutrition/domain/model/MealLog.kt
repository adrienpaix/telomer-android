package health.telomer.android.feature.nutrition.domain.model

enum class MealType(val labelFr: String, val icon: String) {
    BREAKFAST("Petit-déjeuner", "🌅"),
    LUNCH("Déjeuner", "🍽️"),
    DINNER("Dîner", "🌙"),
    SNACK("Collation", "🍎"),
}

data class MealLogItem(
    val id: String,
    val foodItem: FoodItem,
    val quantityG: Double,
    val caloriesKcal: Double? = null,
    val proteinsG: Double? = null,
    val carbsG: Double? = null,
    val fatsG: Double? = null,
)

data class MealLog(
    val id: String,
    val date: String,
    val mealType: MealType,
    val items: List<MealLogItem>,
    val notes: String? = null,
) {
    val totalCalories: Double get() = items.sumOf { it.caloriesKcal ?: 0.0 }
    val totalProteins: Double get() = items.sumOf { it.proteinsG ?: 0.0 }
    val totalCarbs: Double get() = items.sumOf { it.carbsG ?: 0.0 }
    val totalFats: Double get() = items.sumOf { it.fatsG ?: 0.0 }
}

data class NutritionGoal(
    val caloriesKcal: Int? = null,
    val proteinsG: Int? = null,
    val carbsG: Int? = null,
    val fatsG: Int? = null,
)

data class DailySummary(
    val date: String,
    val totalCalories: Double,
    val totalProteins: Double,
    val totalCarbs: Double,
    val totalFats: Double,
    val meals: List<MealLog>,
    val goal: NutritionGoal? = null,
)
