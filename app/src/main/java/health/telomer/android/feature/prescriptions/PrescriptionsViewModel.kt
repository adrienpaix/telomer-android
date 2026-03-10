package health.telomer.android.feature.prescriptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import health.telomer.android.core.data.api.TelomerApi
import health.telomer.android.core.data.api.models.PrescriptionResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PrescriptionsUiState(
    val isLoading: Boolean = true,
    val prescriptions: List<PrescriptionResponse> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class PrescriptionsViewModel @Inject constructor(
    private val api: TelomerApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrescriptionsUiState())
    val uiState: StateFlow<PrescriptionsUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val prescriptions = api.getMyPrescriptions()
                _uiState.value = PrescriptionsUiState(
                    isLoading = false,
                    prescriptions = prescriptions.sortedByDescending { it.createdAt },
                )
            } catch (e: Exception) {
                _uiState.value = PrescriptionsUiState(isLoading = false, error = e.message)
            }
        }
    }
}
