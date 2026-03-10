package health.telomer.android.feature.nutrition.ui.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import health.telomer.android.feature.nutrition.domain.model.*
import health.telomer.android.feature.nutrition.domain.repository.NutritionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class JournalUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val summary: DailySummary? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val showAddOptions: Boolean = false,
)

@HiltViewModel
class NutritionJournalViewModel @Inject constructor(
    private val repository: NutritionRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(JournalUiState())
    val state: StateFlow<JournalUiState> = _state.asStateFlow()

    private val fmt = DateTimeFormatter.ISO_LOCAL_DATE

    init { loadSummary() }

    fun loadSummary() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            repository.getDailySummary(_state.value.selectedDate.format(fmt))
                .onSuccess { summary ->
                    _state.update { it.copy(summary = summary, isLoading = false) }
                }
                .onFailure { e ->
                    _state.update { it.copy(error = e.message, isLoading = false) }
                }
        }
    }

    fun previousDay() {
        _state.update { it.copy(selectedDate = it.selectedDate.minusDays(1)) }
        loadSummary()
    }

    fun nextDay() {
        val next = _state.value.selectedDate.plusDays(1)
        if (!next.isAfter(LocalDate.now())) {
            _state.update { it.copy(selectedDate = next) }
            loadSummary()
        }
    }

    fun toggleAddOptions() {
        _state.update { it.copy(showAddOptions = !it.showAddOptions) }
    }

    fun deleteMealItem(itemId: String) {
        viewModelScope.launch {
            repository.deleteMealItem(itemId).onSuccess { loadSummary() }
        }
    }

    fun deleteMeal(mealId: String) {
        viewModelScope.launch {
            repository.deleteMeal(mealId).onSuccess { loadSummary() }
        }
    }

    val dateLabel: String
        get() {
            val date = _state.value.selectedDate
            return when {
                date == LocalDate.now() -> "Aujourd'hui"
                date == LocalDate.now().minusDays(1) -> "Hier"
                else -> date.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
            }
        }
}
