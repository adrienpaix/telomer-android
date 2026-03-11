package health.telomer.android.feature.appointments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import health.telomer.android.core.data.api.TelomerApi
import health.telomer.android.core.data.api.models.AppointmentResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppointmentsUiState(
    val isLoading: Boolean = true,
    val upcoming: List<AppointmentResponse> = emptyList(),
    val past: List<AppointmentResponse> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class AppointmentsViewModel @Inject constructor(
    private val api: TelomerApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppointmentsUiState())
    val uiState: StateFlow<AppointmentsUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val all = api.getMyAppointments()
                val now = java.time.Instant.now().toString()
                _uiState.value = AppointmentsUiState(
                    isLoading = false,
                    upcoming = all.filter { (it.scheduledAt >= now || it.status == "upcoming" || it.status == "confirmed") && it.status != "cancelled" }
                        .sortedBy { it.scheduledAt },
                    past = all.filter { it.scheduledAt < now && it.status != "upcoming" && it.status != "confirmed" || it.status == "cancelled" }
                        .sortedByDescending { it.scheduledAt },
                )
            } catch (e: Exception) {
                _uiState.value = AppointmentsUiState(isLoading = false, error = e.message)
            }
        }
    }

    fun cancelAppointment(id: String) {
        viewModelScope.launch {
            try {
                api.cancelAppointment(id)
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Erreur lors de l'annulation: ${e.message}")
            }
        }
    }
}
