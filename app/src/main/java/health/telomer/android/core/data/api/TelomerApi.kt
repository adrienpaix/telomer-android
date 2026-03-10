package health.telomer.android.core.data.api

import retrofit2.http.*

/**
 * Retrofit interface for Telomer Health API.
 * Base URL: https://api.telomer.health/api/v1/
 */
interface TelomerApi {
    // ── Appointments ──
    @GET("appointments/mine")
    suspend fun getMyAppointments(): List<Map<String, Any?>>

    // ── Prescriptions ──
    @GET("me/prescriptions")
    suspend fun getMyPrescriptions(): List<Map<String, Any?>>

    // ── Documents ──
    @GET("me/documents")
    suspend fun getMyDocuments(): List<Map<String, Any?>>

    // ── Profile ──
    @GET("me/profile")
    suspend fun getMyProfile(): Map<String, Any?>

    // ── Messages ──
    @GET("messages/mine")
    suspend fun getMyMessages(): List<Map<String, Any?>>

    // ── Practitioners ──
    @GET("practitioners")
    suspend fun getPractitioners(): List<Map<String, Any?>>
}
