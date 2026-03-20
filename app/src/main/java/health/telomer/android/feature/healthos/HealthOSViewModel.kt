package health.telomer.android.feature.healthos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import health.telomer.android.core.data.api.TelomerApi
import health.telomer.android.core.data.api.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HealthOSUiState(
    val isLoading: Boolean = true,
    val dashboard: HealthOSDashboardResponse? = null,
    val selectedPillar: PillarDetailResponse? = null,
    val selectedPillarCode: String? = null,
    val error: String? = null,
)

@HiltViewModel
class HealthOSViewModel @Inject constructor(
    private val api: TelomerApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HealthOSUiState())
    val uiState: StateFlow<HealthOSUiState> = _uiState.asStateFlow()

    init { loadDashboard() }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val dashboard = api.getHealthOSDashboard()
                _uiState.value = _uiState.value.copy(isLoading = false, dashboard = dashboard)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun selectPillar(code: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(selectedPillarCode = code, selectedPillar = null)
            try {
                val detail = api.getHealthOSPillar(code)
                _uiState.value = _uiState.value.copy(selectedPillar = detail)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Impossible de charger ce pilier. Réessayez.",
                    selectedPillarCode = null,
                )
            }
        }
    }

    fun closePillarDetail() {
        _uiState.value = _uiState.value.copy(selectedPillarCode = null, selectedPillar = null)
    }
}
