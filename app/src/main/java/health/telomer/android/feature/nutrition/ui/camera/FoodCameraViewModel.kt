package health.telomer.android.feature.nutrition.ui.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import health.telomer.android.feature.nutrition.domain.repository.NutritionRepository
import health.telomer.android.feature.nutrition.engine.FoodRecognitionEngine
import health.telomer.android.feature.nutrition.engine.RecognizedFood
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CameraUiState(
    val capturedBitmap: Bitmap? = null,
    val isAnalyzing: Boolean = false,
    val recognizedFoods: List<RecognizedFood> = emptyList(),
    val portions: Map<String, Double> = emptyMap(),
    val error: String? = null,
    val addedSuccessfully: Boolean = false,
    val isLoading: Boolean = false,
)

@HiltViewModel
class FoodCameraViewModel @Inject constructor(
    private val engine: FoodRecognitionEngine,
    private val repository: NutritionRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CameraUiState())
    val state: StateFlow<CameraUiState> = _state.asStateFlow()

    private var imageCapture: ImageCapture? = null

    fun setImageCapture(capture: ImageCapture) {
        imageCapture = capture
    }

    fun capturePhoto(context: Context) {
        val capture = imageCapture ?: return
        capture.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val buffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    image.close()
                    bitmap?.let { analyzeImage(it) }
                }

                override fun onError(exception: ImageCaptureException) {
                    _state.update { it.copy(error = exception.message) }
                }
            },
        )
    }

    private fun analyzeImage(bitmap: Bitmap) {
        _state.update { it.copy(capturedBitmap = bitmap, isAnalyzing = true) }
        viewModelScope.launch {
            try {
                val results = engine.analyze(bitmap)
                val portions = results.associate { it.name to it.estimatedPortionG }
                _state.update { it.copy(recognizedFoods = results, portions = portions, isAnalyzing = false) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message, isAnalyzing = false) }
            }
        }
    }

    fun updatePortion(foodName: String, grams: Double) {
        _state.update { it.copy(portions = it.portions + (foodName to grams)) }
    }

    fun retake() {
        _state.update { CameraUiState() }
    }

    fun addToMeal(foodName: String, calories: Double?, proteinG: Double?, carbsG: Double?, fatG: Double?) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                repository.createDirectFoodLog(
                    foodName = foodName,
                    calories = calories,
                    proteinG = proteinG,
                    carbsG = carbsG,
                    fatG = fatG,
                    source = "photo",
                )
                _state.update { it.copy(isLoading = false, addedSuccessfully = true) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    /** Convenience overload — adds all recognized foods from the current analysis. */
    fun addRecognizedFoodsToMeal() {
        val foods = _state.value.recognizedFoods
        val portions = _state.value.portions
        if (foods.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                for (food in foods) {
                    val portionG = portions[food.name] ?: food.estimatedPortionG
                    repository.createDirectFoodLog(
                        foodName = food.name,
                        calories = food.estimatedCalories,
                        proteinG = food.estimatedProteins,
                        carbsG = food.estimatedCarbs,
                        fatG = food.estimatedFats,
                        source = "photo",
                    )
                }
                _state.update { it.copy(isLoading = false, addedSuccessfully = true) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
