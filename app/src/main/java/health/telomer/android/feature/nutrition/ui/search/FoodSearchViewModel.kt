package health.telomer.android.feature.nutrition.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import health.telomer.android.feature.nutrition.domain.model.FoodItem
import health.telomer.android.feature.nutrition.domain.model.MealType
import health.telomer.android.feature.nutrition.domain.repository.NutritionRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.net.SocketTimeoutException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val results: List<FoodItem> = emptyList(),
    val isLoading: Boolean = false,
    val selectedFood: FoodItem? = null,
    val quantityG: Double = 100.0,
    val selectedMealType: MealType = MealType.LUNCH,
    val error: String? = null,
    val addedSuccessfully: Boolean = false,
)

@OptIn(FlowPreview::class)
@HiltViewModel
class FoodSearchViewModel @Inject constructor(
    private val repository: NutritionRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    private val _queryFlow = MutableStateFlow("")

    init {
        _queryFlow
            .debounce(300)
            .filter { it.length >= 2 }
            .distinctUntilChanged()
            .onEach { query ->
                _state.update { it.copy(isLoading = true, error = null) }
                repository.searchFood(query)
                    .onSuccess { results ->
                        _state.update { it.copy(results = results, isLoading = false) }
                    }
                    .onFailure { e ->
                        val message = when (e) {
                            is SocketTimeoutException -> "La recherche prend plus de temps que prévu. Réessayez."
                            else -> e.message ?: "Erreur inconnue"
                        }
                        _state.update { it.copy(error = message, isLoading = false, results = emptyList()) }
                    }
            }
            .launchIn(viewModelScope)
    }

    fun updateQuery(query: String) {
        _state.update { it.copy(query = query) }
        _queryFlow.value = query
    }

    fun selectFood(food: FoodItem) {
        _state.update { it.copy(selectedFood = food, quantityG = 100.0) }
    }

    fun clearSelection() {
        _state.update { it.copy(selectedFood = null) }
    }

    fun updateQuantity(grams: Double) {
        _state.update { it.copy(quantityG = grams) }
    }

    fun updateMealType(type: MealType) {
        _state.update { it.copy(selectedMealType = type) }
    }

    fun addToMeal() {
        val food = _state.value.selectedFood ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            repository.addMealItem(
                date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                mealType = _state.value.selectedMealType,
                foodItemId = food.id,
                quantityG = _state.value.quantityG,
            ).onSuccess {
                _state.update { it.copy(addedSuccessfully = true, isLoading = false, selectedFood = null) }
            }.onFailure { e ->
                val message = when (e) {
                    is SocketTimeoutException -> "La requête a pris trop de temps. Réessayez."
                    else -> e.message ?: "Erreur inconnue"
                }
                _state.update { it.copy(error = message, isLoading = false) }
            }
        }
    }
}
