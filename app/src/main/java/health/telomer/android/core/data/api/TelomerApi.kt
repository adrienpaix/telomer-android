package health.telomer.android.core.data.api

import health.telomer.android.core.data.api.models.*
import okhttp3.MultipartBody
import retrofit2.http.*

/**
 * Retrofit interface for Telomer Health API.
 * Base URL: https://api.telomer.health/api/v1/
 */
interface TelomerApi {

    // ── Appointments ──
    @GET("appointments/mine")
    suspend fun getMyAppointments(): List<AppointmentResponse>

    @POST("appointments/book")
    suspend fun bookAppointment(@Body request: BookAppointmentRequest): AppointmentResponse

    @DELETE("appointments/{id}")
    suspend fun cancelAppointment(@Path("id") id: String)

    // ── Consultation Room (LiveKit) ──
    @POST("consultations/room/{appointmentId}")
    suspend fun createConsultationRoom(@Path("appointmentId") appointmentId: String): ConsultationRoomResponse

    // ── Practitioners & Availability ──
    @GET("practitioners")
    suspend fun getPractitioners(): List<PractitionerResponse>

    @GET("practitioners/{id}")
    suspend fun getPractitioner(@Path("id") id: String): PractitionerResponse

    @GET("practitioners/{id}/availability")
    suspend fun getAvailability(
        @Path("id") practitionerId: String,
        @Query("from") fromDate: String? = null,
        @Query("to") toDate: String? = null,
    ): List<DayAvailability>

    // ── Prescriptions (patient) ──
    @GET("patient/prescriptions")
    suspend fun getMyPrescriptions(): List<PrescriptionResponse>

    // ── Documents ──
    @GET("me/documents")
    suspend fun getMyDocuments(): List<DocumentResponse>

    @Multipart
    @POST("me/documents")
    suspend fun uploadDocument(@Part file: MultipartBody.Part): DocumentResponse

    // ── Profile ──
    @GET("me/patient-profile")
    suspend fun getMyProfile(): PatientProfile

    @PUT("me/patient-profile")
    suspend fun updateProfile(@Body request: ProfileUpdateRequest): PatientProfile

    // ── Messages ──
    @GET("messages/conversations")
    suspend fun getConversations(): List<ConversationResponse>

    @GET("messages/recipients")
    suspend fun getRecipients(): List<RecipientResponse>

    @GET("messages/{userId}")
    suspend fun getMessages(
        @Path("userId") userId: String,
    ): List<MessageResponse>

    @POST("messages/{userId}")
    suspend fun sendMessage(
        @Path("userId") userId: String,
        @Body request: SendMessageRequest,
    ): MessageResponse
}
