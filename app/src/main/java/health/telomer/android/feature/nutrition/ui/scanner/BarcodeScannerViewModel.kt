package health.telomer.android.feature.nutrition.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import health.telomer.android.feature.nutrition.domain.model.FoodItem
import health.telomer.android.feature.nutrition.domain.repository.NutritionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScannerUiState(
    val scannedFood: FoodItem? = null,
    val quantityG: Double = 100.0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val lastScannedEan: String? = null,
)

@HiltViewModel
class BarcodeScannerViewModel @Inject constructor(
    private val repository: NutritionRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ScannerUiState())
    val state: StateFlow<ScannerUiState> = _state.asStateFlow()

    fun onBarcodeDetected(ean: String) {
        // Avoid duplicate lookups
        if (ean == _state.value.lastScannedEan || _state.value.isLoading) return
        _state.update { it.copy(lastScannedEan = ean, isLoading = true, error = null) }

        viewModelScope.launch {
            repository.getFoodByBarcode(ean)
                .onSuccess { food ->
                    _state.update { it.copy(scannedFood = food, isLoading = false) }
                }
                .onFailure { e ->
                    _state.update { it.copy(error = "Produit non trouvé (${ean})", isLoading = false) }
                }
        }
    }

    fun updateQuantity(grams: Double) {
        _state.update { it.copy(quantityG = grams) }
    }

    fun rescan() {
        _state.update { ScannerUiState() }
    }

    fun addToMeal() {
        // TODO: call repository to add scannedFood with quantityG to meal
    }
}
