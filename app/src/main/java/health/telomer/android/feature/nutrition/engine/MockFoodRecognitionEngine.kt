package health.telomer.android.feature.nutrition.engine

import android.graphics.Bitmap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MVP placeholder — returns empty list (pas d'IA embarquée disponible).
 * La reconnaissance visuelle est g\u00e9r\u00e9e c\u00f4t\u00e9 serveur via l'API food-log.
 * Remplacer par une impl ONNX ou cloud quand disponible.
 */
@Singleton
class MockFoodRecognitionEngine @Inject constructor() : FoodRecognitionEngine {

    override suspend fun analyze(bitmap: Bitmap): List<RecognizedFood> {
        return emptyList()
    }
}
