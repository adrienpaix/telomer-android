package health.telomer.android.core.data.api.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AppointmentResponse(
    val id: String,
    @Json(name = "scheduled_at") val scheduledAt: String,
    val type: String? = null,
    val status: String? = null,
    @Json(name = "doctor_id") val doctorId: String? = null,
    @Json(name = "patient_id") val patientId: String? = null,
    @Json(name = "duration_min") val durationMin: Int? = 30,
    @Json(name = "practitioner_name") val practitionerName: String? = null,
    @Json(name = "patient_name") val patientName: String? = null,
    @Json(name = "meet_link") val meetLink: String? = null,
    val notes: String? = null,
)

@JsonClass(generateAdapter = true)
data class PrescriptionResponse(
    val id: String,
    @Json(name = "practitioner_id") val practitionerId: String? = null,
    @Json(name = "patient_id") val patientId: String? = null,
    val title: String? = null,
    val content: String? = null,
    val medications: List<Any>? = null,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "is_signed") val isSigned: Boolean? = null,
    @Json(name = "pdf_url") val pdfUrl: String? = null,
    @Json(name = "practitioner_name") val practitionerName: String? = null,
    @Json(name = "practitioner_specialty") val practitionerSpecialty: String? = null,
)

@JsonClass(generateAdapter = true)
data class DocumentResponse(
    val id: String,
    @Json(name = "file_name") val fileName: String,
    @Json(name = "file_type") val fileType: String? = null,
    @Json(name = "file_size_bytes") val fileSizeBytes: Long? = null,
    @Json(name = "document_type") val documentType: String? = null,
    val description: String? = null,
    @Json(name = "document_date") val documentDate: String? = null,
    @Json(name = "uploaded_at") val uploadedAt: String? = null,
)

@JsonClass(generateAdapter = true)
data class ConsultationRoomResponse(
    @Json(name = "room_name") val roomName: String,
    @Json(name = "livekit_url") val livekitUrl: String? = null,
    @Json(name = "doctor_token") val doctorToken: String? = null,
    @Json(name = "patient_token") val patientToken: String? = null,
    @Json(name = "report_id") val reportId: String? = null,
)

@JsonClass(generateAdapter = true)
data class MessageResponse(
    val id: String,
    val content: String,
    @Json(name = "sender_id") val senderId: String,
    @Json(name = "recipient_id") val recipientId: String? = null,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "is_read") val isRead: Boolean? = null,
    @Json(name = "sender_name") val senderName: String? = null,
    val subject: String? = null,
    @Json(name = "attachment_url") val attachmentUrl: String? = null,
    @Json(name = "attachment_name") val attachmentName: String? = null,
)

@JsonClass(generateAdapter = true)
data class ConversationResponse(
    @Json(name = "user_id") val userId: String,
    @Json(name = "user_name") val userName: String,
    @Json(name = "last_message") val lastMessage: String? = null,
    @Json(name = "last_at") val lastAt: String? = null,
    @Json(name = "unread_count") val unreadCount: Int = 0,
)

@JsonClass(generateAdapter = true)
data class RecipientResponse(
    val id: String,
    val name: String,
    val role: String,
    val email: String? = null,
)

@JsonClass(generateAdapter = true)
data class PractitionerResponse(
    val id: String? = null,
    @Json(name = "user_id") val userId: String,
    @Json(name = "first_name") val firstName: String,
    @Json(name = "last_name") val lastName: String,
    val specialty: String? = null,
    @Json(name = "photo_url") val photoUrl: String? = null,
    @Json(name = "bio_presentation") val bioPresentation: String? = null,
    @Json(name = "bio_experience") val bioExperience: String? = null,
    @Json(name = "bio_formation") val bioFormation: String? = null,
    @Json(name = "learned_societies") val learnedSocieties: String? = null,
    @Json(name = "consultation_price") val consultationPrice: Double? = null,
    @Json(name = "is_active") val isActive: Boolean? = true,
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
data class DayAvailability(
    val date: String,
    val slots: List<String>,
)

@JsonClass(generateAdapter = true)
data class BookAppointmentRequest(
    @Json(name = "practitioner_profile_id") val practitionerProfileId: String,
    @Json(name = "scheduled_at") val scheduledAt: String,
    val type: String = "initial",
    @Json(name = "duration_min") val durationMin: Int = 30,
)

@JsonClass(generateAdapter = true)
data class SendMessageRequest(
    val content: String,
    val subject: String? = null,
)

@JsonClass(generateAdapter = true)
data class ProfileUpdateRequest(
    val phone: String? = null,
    val address: String? = null,
)

@JsonClass(generateAdapter = true)
data class ActionPlanResponse(
    val id: String,
    @Json(name = "patient_id") val patientId: String? = null,
    val content: String,
    @Json(name = "created_at") val createdAt: String,
)

// ── Health Metrics Bulk Sync ──────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class HealthMetricItem(
    @Json(name = "metric_type") val metric_type: String,
    val value: Double,
    val unit: String?,
    @Json(name = "recorded_at") val recorded_at: String,
    val source: String = "health_connect",
)

@JsonClass(generateAdapter = true)
data class BulkMetricsPayload(
    val metrics: List<HealthMetricItem>,
)
