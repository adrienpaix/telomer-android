package health.telomer.android.feature.nutrition.engine

import android.graphics.Bitmap
import android.graphics.Rect

/**
 * Interface for food recognition from camera images.
 * MVP uses [MockFoodRecognitionEngine]; replace with ONNX-backed impl later.
 */
interface FoodRecognitionEngine {
    suspend fun analyze(bitmap: Bitmap): List<RecognizedFood>
}

data class RecognizedFood(
    val name: String,
    val confidence: Float,
    val boundingBox: Rect? = null,
    val estimatedPortionG: Double = 100.0,
    val estimatedCalories: Double? = null,
    val estimatedProteins: Double? = null,
    val estimatedCarbs: Double? = null,
    val estimatedFats: Double? = null,
)
