package health.telomer.android.feature.dashboard.data

import health.telomer.android.core.data.api.TelomerApi
import health.telomer.android.core.data.api.models.AppointmentResponse
import health.telomer.android.core.data.api.models.ConversationResponse
import health.telomer.android.core.data.api.models.HealthOSDashboardResponse
import health.telomer.android.core.data.api.models.PatientProfile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DashboardRepository @Inject constructor(private val api: TelomerApi) {
    suspend fun getProfile(): PatientProfile = api.getMyProfile()
    suspend fun getUpcomingAppointments(): List<AppointmentResponse>? =
        runCatching { api.getMyAppointments() }.getOrNull()
    suspend fun getConversations(): List<ConversationResponse>? =
        runCatching { api.getConversations() }.getOrNull()
    suspend fun getHealthOSDashboard(): HealthOSDashboardResponse? =
        runCatching { api.getHealthOSDashboard() }.getOrNull()
}
