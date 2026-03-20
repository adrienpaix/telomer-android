package health.telomer.android.feature.healthconnect.domain

import java.time.Instant

data class HealthMetric(
    val type: MetricType,
    val value: Double,
    val unit: String,
    val recordedAt: Instant,
    val metadata: Map<String, Double> = emptyMap(), // extra fields (e.g. sleep stages)
)

enum class MetricType(
    val apiName: String,
    val label: String,
    val unit: String,
    val icon: String,
) {
    STEPS("steps", "Pas", "pas", "🚶"),
    HEART_RATE("heart_rate", "Fréquence cardiaque", "bpm", "❤️"),
    RESTING_HEART_RATE("resting_heart_rate", "FC au repos", "bpm", "💓"),
    HRV("hrv", "VFC (HRV)", "ms", "📊"),
    SLEEP("sleep", "Sommeil", "minutes", "😴"),
    WEIGHT("weight", "Poids", "kg", "⚖️"),
    BODY_FAT("body_fat", "Masse grasse", "%", "📉"),
    LEAN_MASS("lean_mass", "Masse maigre", "kg", "💪"),
    BODY_WATER("body_water", "Eau corporelle", "%", "💧"),
    ACTIVE_CALORIES("active_calories", "Calories actives", "kcal", "🔥"),
    EXERCISE("exercise", "Exercice", "minutes", "🏃"),
    SPO2("spo2", "SpO2", "%", "🫁"),
    BODY_TEMPERATURE("body_temperature", "Température", "°C", "🌡️"),
    HEART_ZONE_1("heart_zone_1", "Zone FC 1 (récupération)", "min", "💚"),
    HEART_ZONE_2("heart_zone_2", "Zone FC 2 (endurance)", "min", "💛"),
    HEART_ZONE_3("heart_zone_3", "Zone FC 3 (aérobie)", "min", "🟠"),
    HEART_ZONE_4("heart_zone_4", "Zone FC 4 (anaérobie)", "min", "🔴"),
    HEART_ZONE_5("heart_zone_5", "Zone FC 5 (max)", "min", "❗"),
}
