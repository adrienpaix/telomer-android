package health.telomer.android.feature.healthconnect.domain

import java.time.Instant

data class HealthMetric(
    val type: MetricType,
    val value: Double,
    val unit: String,
    val recordedAt: Instant,
)

enum class MetricType(
    val apiName: String,
    val label: String,
    val unit: String,
    val icon: String,
) {
    STEPS("steps", "Pas", "pas", "🚶"),
    HEART_RATE("heart_rate", "Fréquence cardiaque", "bpm", "❤️"),
    SLEEP("sleep", "Sommeil", "minutes", "😴"),
    WEIGHT("weight", "Poids", "kg", "⚖️"),
    ACTIVE_CALORIES("active_calories", "Calories actives", "kcal", "🔥"),
    EXERCISE("exercise", "Exercice", "minutes", "🏃"),
}
