package health.telomer.android.feature.nutrition.engine

import android.graphics.Bitmap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mock implementation — returns empty results to indicate AI is not yet available.
 * Will be replaced by a real ONNX/cloud-based engine later.
 */
@Singleton
class MockFoodRecognitionEngine @Inject constructor() : FoodRecognitionEngine {

    override suspend fun analyze(bitmap: Bitmap): List<RecognizedFood> {
        // Return empty list to indicate no AI available
        return emptyList()
    }
}
