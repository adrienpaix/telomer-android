package health.telomer.android.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import health.telomer.android.core.data.api.TelomerApi
import health.telomer.android.core.data.api.models.AppointmentResponse
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = true,
    val firstName: String = "",
    val nextAppointment: AppointmentResponse? = null,
    val unreadMessages: Int = 0,
    val questionnaireStatus: String? = null,
    val error: String? = null,
    val healthOSScore: Double? = null,
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
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                coroutineScope {
                    val profileDeferred = async { api.getMyProfile() }
                    val apptsDeferred = async { runCatching { api.getMyAppointments() }.getOrElse { emptyList() } }
                    val messagesDeferred = async { runCatching { api.getConversations() }.getOrElse { emptyList() } }
                    val healthDeferred = async { runCatching { api.getHealthOSDashboard() }.getOrNull() }

                    val profile = profileDeferred.await()
                    val appointments = apptsDeferred.await()
                    val conversations = messagesDeferred.await()
                    val health = healthDeferred.await()

                    val now = java.time.Instant.now().toString()
                    val nextAppt = appointments
                        .filter { it.status != "cancelled" && it.scheduledAt >= now }
                        .minByOrNull { it.scheduledAt }

                    val unread = conversations.sumOf { it.unreadCount }

                    _uiState.value = DashboardUiState(
                        isLoading = false,
                        firstName = profile.firstName,
                        nextAppointment = nextAppt,
                        unreadMessages = unread,
                        questionnaireStatus = null,
                        healthOSScore = health?.globalScore,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Erreur de chargement",
                    )
                }
            }
        }
    }
}
