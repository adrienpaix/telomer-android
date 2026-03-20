package health.telomer.android.feature.appointments.data

import health.telomer.android.core.data.api.TelomerApi
import health.telomer.android.core.data.api.models.AppointmentResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppointmentsRepository @Inject constructor(private val api: TelomerApi) {
    suspend fun getAppointments(): List<AppointmentResponse> = api.getMyAppointments()
    suspend fun cancelAppointment(id: String) = api.cancelAppointment(id)
}
