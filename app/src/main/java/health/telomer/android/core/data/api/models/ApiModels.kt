package health.telomer.android.core.data.api.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AppointmentResponse(
    val id: String,
    val date: String,
    val type: String? = null,
    val status: String? = null,
    @Json(name = "doctor_name") val doctorName: String? = null,
    @Json(name = "doctor_first_name") val doctorFirstName: String? = null,
    @Json(name = "doctor_last_name") val doctorLastName: String? = null,
    @Json(name = "doctor_specialty") val doctorSpecialty: String? = null,
)

@JsonClass(generateAdapter = true)
data class PrescriptionResponse(
    val id: String,
    @Json(name = "created_at") val createdAt: String,
    val medications: String? = null,
    @Json(name = "doctor_name") val doctorName: String? = null,
    @Json(name = "pdf_url") val pdfUrl: String? = null,
)

@JsonClass(generateAdapter = true)
data class DocumentResponse(
    val id: String,
    val filename: String,
    @Json(name = "uploaded_at") val uploadedAt: String? = null,
    @Json(name = "document_date") val documentDate: String? = null,
    @Json(name = "file_type") val fileType: String? = null,
    @Json(name = "file_url") val fileUrl: String? = null,
)

@JsonClass(generateAdapter = true)
data class MessageResponse(
    val id: String,
    val content: String,
    @Json(name = "sender_id") val senderId: String,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "is_read") val isRead: Boolean? = null,
    @Json(name = "sender_name") val senderName: String? = null,
)

@JsonClass(generateAdapter = true)
data class ConversationResponse(
    val id: String,
    @Json(name = "practitioner_id") val practitionerId: String,
    @Json(name = "practitioner_name") val practitionerName: String? = null,
    @Json(name = "last_message") val lastMessage: String? = null,
    @Json(name = "last_message_at") val lastMessageAt: String? = null,
    @Json(name = "unread_count") val unreadCount: Int = 0,
)

@JsonClass(generateAdapter = true)
data class PractitionerResponse(
    @Json(name = "user_id") val userId: String,
    @Json(name = "first_name") val firstName: String,
    @Json(name = "last_name") val lastName: String,
    val specialties: List<String>? = null,
    @Json(name = "photo_url") val photoUrl: String? = null,
    val bio: String? = null,
)

@JsonClass(generateAdapter = true)
data class PatientProfile(
    @Json(name = "first_name") val firstName: String,
    @Json(name = "last_name") val lastName: String,
    val email: String,
    val phone: String? = null,
    val address: String? = null,
    @Json(name = "date_of_birth") val dateOfBirth: String? = null,
    val gender: String? = null,
)

@JsonClass(generateAdapter = true)
data class AvailableSlot(
    val date: String,
    val time: String,
    @Json(name = "practitioner_id") val practitionerId: String? = null,
)

@JsonClass(generateAdapter = true)
data class BookAppointmentRequest(
    @Json(name = "practitioner_id") val practitionerId: String,
    val date: String,
    val time: String,
    val type: String = "consultation",
)

@JsonClass(generateAdapter = true)
data class SendMessageRequest(
    @Json(name = "conversation_id") val conversationId: String,
    val content: String,
)

@JsonClass(generateAdapter = true)
data class DashboardSummary(
    @Json(name = "next_appointment") val nextAppointment: AppointmentResponse? = null,
    @Json(name = "unread_messages") val unreadMessages: Int = 0,
    @Json(name = "questionnaire_status") val questionnaireStatus: String? = null,
)

@JsonClass(generateAdapter = true)
data class ProfileUpdateRequest(
    val phone: String? = null,
    val address: String? = null,
)
