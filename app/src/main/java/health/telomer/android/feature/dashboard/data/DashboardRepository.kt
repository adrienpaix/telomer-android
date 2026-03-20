package health.telomer.android.feature.dashboard.data

import android.util.Log
import health.telomer.android.BuildConfig
import health.telomer.android.core.data.api.TelomerApi
import health.telomer.android.core.data.api.models.AppointmentResponse
import health.telomer.android.core.data.api.models.ConversationResponse
import health.telomer.android.core.data.api.models.HealthOSDashboardResponse
import health.telomer.android.core.data.api.models.PatientProfile
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "DashboardRepo"

@Singleton
class DashboardRepository @Inject constructor(private val api: TelomerApi) {
    suspend fun getProfile(): PatientProfile = api.getMyProfile()

    suspend fun getUpcomingAppointments(): List<AppointmentResponse>? =
        runCatching { api.getMyAppointments() }
            .onFailure { if (BuildConfig.DEBUG) Log.w(TAG, "Failed to load appointments", it) }
            .getOrNull()

    suspend fun getConversations(): List<ConversationResponse>? =
        runCatching { api.getConversations() }
            .onFailure { if (BuildConfig.DEBUG) Log.w(TAG, "Failed to load conversations", it) }
            .getOrNull()

    suspend fun getHealthOSDashboard(): HealthOSDashboardResponse? =
        runCatching { api.getHealthOSDashboard() }
            .onFailure { if (BuildConfig.DEBUG) Log.e(TAG, "Failed to load HealthOS dashboard", it) }
            .getOrNull()
}
