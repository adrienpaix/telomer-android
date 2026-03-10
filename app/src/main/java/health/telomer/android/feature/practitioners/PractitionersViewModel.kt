package health.telomer.android.feature.practitioners

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import health.telomer.android.core.data.api.TelomerApi
import health.telomer.android.core.data.api.models.PractitionerResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PractitionersUiState(
    val isLoading: Boolean = true,
    val practitioners: List<PractitionerResponse> = emptyList(),
    val selectedPractitioner: PractitionerResponse? = null,
    val error: String? = null,
)

@HiltViewModel
class PractitionersViewModel @Inject constructor(
    private val api: TelomerApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PractitionersUiState())
    val uiState: StateFlow<PractitionersUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val practitioners = api.getPractitioners()
                _uiState.value = PractitionersUiState(isLoading = false, practitioners = practitioners)
            } catch (e: Exception) {
                _uiState.value = PractitionersUiState(isLoading = false, error = e.message)
            }
        }
    }

    fun selectPractitioner(practitioner: PractitionerResponse) {
        _uiState.value = _uiState.value.copy(selectedPractitioner = practitioner)
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedPractitioner = null)
    }
}
