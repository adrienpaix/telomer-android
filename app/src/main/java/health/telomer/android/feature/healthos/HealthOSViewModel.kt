package health.telomer.android.feature.healthos

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import health.telomer.android.BuildConfig
import health.telomer.android.core.data.api.models.*
import health.telomer.android.feature.healthos.data.HealthOSRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "HealthOSVM"

data class HealthOSUiState(
    val isLoading: Boolean = true,
    val dashboard: HealthOSDashboardResponse? = null,
    val selectedPillar: PillarDetailResponse? = null,
    val selectedPillarCode: String? = null,
    val error: String? = null,
)

@HiltViewModel
class HealthOSViewModel @Inject constructor(
    private val repository: HealthOSRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HealthOSUiState())
    val uiState: StateFlow<HealthOSUiState> = _uiState.asStateFlow()

    init { loadDashboard() }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val dashboard = repository.getDashboard()
                _uiState.value = _uiState.value.copy(isLoading = false, dashboard = dashboard)
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) Log.e(TAG, "Failed to load dashboard", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Impossible de charger le bilan : ${e.localizedMessage ?: e.message ?: "erreur inconnue"}",
                )
            }
        }
    }

    fun selectPillar(code: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(selectedPillarCode = code, selectedPillar = null)
            try {
                val detail = repository.getPillar(code)
                _uiState.value = _uiState.value.copy(selectedPillar = detail)
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) Log.e(TAG, "Failed to load pillar: $code", e)
                _uiState.value = _uiState.value.copy(
                    error = "Impossible de charger ce pilier : ${e.localizedMessage ?: e.message ?: "erreur inconnue"}",
                    selectedPillarCode = null,
                )
            }
        }
    }

    fun closePillarDetail() {
        _uiState.value = _uiState.value.copy(selectedPillarCode = null, selectedPillar = null)
    }
}
