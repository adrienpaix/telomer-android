package health.telomer.android.feature.nutrition.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import health.telomer.android.feature.nutrition.domain.model.NutritionGoal
import health.telomer.android.feature.nutrition.domain.repository.NutritionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GoalsUiState(
    val calories: Int = 2000,
    val proteins: Int = 60,
    val carbs: Int = 250,
    val fats: Int = 70,
    val isLoading: Boolean = false,
    val isEditing: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class NutritionGoalsViewModel @Inject constructor(
    private val repository: NutritionRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(GoalsUiState())
    val state: StateFlow<GoalsUiState> = _state.asStateFlow()

    init { loadGoals() }

    private fun loadGoals() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            repository.getGoals()
                .onSuccess { goal ->
                    _state.update {
                        it.copy(
                            calories = goal.caloriesKcal ?: 2000,
                            proteins = goal.proteinsG ?: 60,
                            carbs = goal.carbsG ?: 250,
                            fats = goal.fatsG ?: 70,
                            isLoading = false,
                        )
                    }
                }
                .onFailure { _state.update { it.copy(isLoading = false) } }
        }
    }

    fun toggleEdit() { _state.update { it.copy(isEditing = !it.isEditing, saved = false) } }

    fun updateCalories(v: Int) { _state.update { it.copy(calories = v) } }
    fun updateProteins(v: Int) { _state.update { it.copy(proteins = v) } }
    fun updateCarbs(v: Int) { _state.update { it.copy(carbs = v) } }
    fun updateFats(v: Int) { _state.update { it.copy(fats = v) } }

    fun save() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val goal = NutritionGoal(
                caloriesKcal = _state.value.calories,
                proteinsG = _state.value.proteins,
                carbsG = _state.value.carbs,
                fatsG = _state.value.fats,
            )
            repository.updateGoals(goal)
                .onSuccess { _state.update { it.copy(isLoading = false, isEditing = false, saved = true) } }
                .onFailure { e -> _state.update { it.copy(error = e.message, isLoading = false) } }
        }
    }
}
