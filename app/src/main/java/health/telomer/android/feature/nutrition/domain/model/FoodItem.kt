package health.telomer.android.feature.nutrition.domain.model

data class FoodItem(
    val id: String,
    val name: String,
    val nameFr: String? = null,
    val barcode: String? = null,
    val caloriesKcal: Double? = null,
    val proteinsG: Double? = null,
    val carbsG: Double? = null,
    val fatsG: Double? = null,
    val fiberG: Double? = null,
    val imageUrl: String? = null,
) {
    /** Display name — prefer French */
    val displayName: String get() = nameFr ?: name
}
