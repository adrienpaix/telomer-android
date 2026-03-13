package health.telomer.android.feature.dashboard

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

data class DashboardUiState(
    val isLoading: Boolean = true,
    val firstName: String = "",
    val nextAppointment: AppointmentResponse? = null,
    val unreadMessages: Int = 0,
    val questionnaireStatus: String? = null,
    val error: String? = null,
    val healthOSScore: Int? = null,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val api: TelomerApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val profile = api.getMyProfile()
                val appointments = try { api.getMyAppointments() } catch (_: Exception) { emptyList() }
                val conversations = try { api.getConversations() } catch (_: Exception) { emptyList() }

                val now = java.time.Instant.now().toString()
                val nextAppt = appointments
                    .filter { it.status != "cancelled" && it.scheduledAt >= now }
                    .minByOrNull { it.scheduledAt }

                val unread = conversations.sumOf { it.unreadCount }

                val healthOSScore = try { api.getHealthOSDashboard().globalScore } catch (_: Exception) { null }

                _uiState.value = DashboardUiState(
                    isLoading = false,
                    firstName = profile.firstName,
                    nextAppointment = nextAppt,
                    unreadMessages = unread,
                    questionnaireStatus = null,
                    healthOSScore = healthOSScore,
                )
            } catch (e: Exception) {
                _uiState.value = DashboardUiState(
                    isLoading = false,
                    error = e.message ?: "Erreur de chargement",
                )
            }
        }
    }
}
