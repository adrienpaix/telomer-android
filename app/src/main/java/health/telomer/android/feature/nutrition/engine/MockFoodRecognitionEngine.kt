package health.telomer.android.feature.nutrition.engine

import android.graphics.Bitmap
import android.graphics.Rect
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mock implementation for dev/test.
 * Simulates a 1-second analysis and returns fake results.
 */
@Singleton
class MockFoodRecognitionEngine @Inject constructor() : FoodRecognitionEngine {

    private val mockFoods = listOf(
        RecognizedFood(
            name = "Poulet grillé",
            confidence = 0.92f,
            boundingBox = Rect(50, 80, 350, 320),
            estimatedPortionG = 150.0,
            estimatedCalories = 248.0,
            estimatedProteins = 38.0,
            estimatedCarbs = 0.0,
            estimatedFats = 10.0,
        ),
        RecognizedFood(
            name = "Riz blanc",
            confidence = 0.87f,
            boundingBox = Rect(360, 100, 600, 300),
            estimatedPortionG = 200.0,
            estimatedCalories = 260.0,
            estimatedProteins = 5.0,
            estimatedCarbs = 58.0,
            estimatedFats = 0.5,
        ),
        RecognizedFood(
            name = "Brocoli vapeur",
            confidence = 0.78f,
            boundingBox = Rect(100, 340, 320, 500),
            estimatedPortionG = 100.0,
            estimatedCalories = 34.0,
            estimatedProteins = 2.8,
            estimatedCarbs = 7.0,
            estimatedFats = 0.4,
        ),
    )

    override suspend fun analyze(bitmap: Bitmap): List<RecognizedFood> {
        delay(1200) // simulate processing
        return mockFoods.shuffled().take((1..3).random())
    }
}
