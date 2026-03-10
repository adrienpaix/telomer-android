package health.telomer.android.core.data.api

import health.telomer.android.core.data.api.models.*
import okhttp3.MultipartBody
import retrofit2.http.*

/**
 * Retrofit interface for Telomer Health API.
 * Base URL: https://api.telomer.health/api/v1/
 */
interface TelomerApi {

    // ── Dashboard ──
    @GET("me/dashboard")
    suspend fun getDashboard(): DashboardSummary

    // ── Appointments ──
    @GET("appointments/mine")
    suspend fun getMyAppointments(): List<AppointmentResponse>

    @POST("appointments")
    suspend fun bookAppointment(@Body request: BookAppointmentRequest): AppointmentResponse

    @DELETE("appointments/{id}")
    suspend fun cancelAppointment(@Path("id") id: String)

    @GET("practitioners/{id}/slots")
    suspend fun getAvailableSlots(
        @Path("id") practitionerId: String,
        @Query("date") date: String? = null,
    ): List<AvailableSlot>

    // ── Prescriptions ──
    @GET("me/prescriptions")
    suspend fun getMyPrescriptions(): List<PrescriptionResponse>

    // ── Documents ──
    @GET("me/documents")
    suspend fun getMyDocuments(): List<DocumentResponse>

    @Multipart
    @POST("me/documents")
    suspend fun uploadDocument(@Part file: MultipartBody.Part): DocumentResponse

    // ── Profile ──
    @GET("me/profile")
    suspend fun getMyProfile(): PatientProfile

    @PATCH("me/profile")
    suspend fun updateProfile(@Body request: ProfileUpdateRequest): PatientProfile

    // ── Messages ──
    @GET("messages/conversations")
    suspend fun getConversations(): List<ConversationResponse>

    @GET("messages/conversations/{id}")
    suspend fun getConversationMessages(
        @Path("id") conversationId: String,
    ): List<MessageResponse>

    @POST("messages")
    suspend fun sendMessage(@Body request: SendMessageRequest): MessageResponse

    // ── Practitioners ──
    @GET("practitioners")
    suspend fun getPractitioners(): List<PractitionerResponse>

    @GET("practitioners/{id}")
    suspend fun getPractitioner(@Path("id") id: String): PractitionerResponse
}
