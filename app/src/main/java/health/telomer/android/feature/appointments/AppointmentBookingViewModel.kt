package health.telomer.android.feature.appointments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import health.telomer.android.core.data.api.TelomerApi
import health.telomer.android.core.data.api.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class BookingUiState(
    val isLoading: Boolean = true,
    val practitioners: List<PractitionerResponse> = emptyList(),
    val selectedPractitioner: PractitionerResponse? = null,
    val availability: List<DayAvailability> = emptyList(),
    val selectedDate: String? = null,
    val selectedTime: String? = null,
    val isBooking: Boolean = false,
    val bookingConfirmed: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class AppointmentBookingViewModel @Inject constructor(
    private val api: TelomerApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookingUiState())
    val uiState: StateFlow<BookingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val practitioners = api.getPractitioners()
                _uiState.value = BookingUiState(isLoading = false, practitioners = practitioners)
            } catch (e: Exception) {
                _uiState.value = BookingUiState(isLoading = false, error = e.message)
            }
        }
    }

    fun selectPractitioner(practitioner: PractitionerResponse) {
        _uiState.value = _uiState.value.copy(
            selectedPractitioner = practitioner,
            selectedDate = null,
            selectedTime = null,
            availability = emptyList(),
        )
        viewModelScope.launch {
            try {
                // Use practitioner profile id for availability
                val practId = practitioner.id ?: practitioner.userId
                val fromDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                val availability = api.getAvailability(practId, fromDate = fromDate)
                _uiState.value = _uiState.value.copy(availability = availability)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Impossible de charger les créneaux: ${e.message}")
            }
        }
    }

    fun selectSlot(date: String, time: String) {
        _uiState.value = _uiState.value.copy(selectedDate = date, selectedTime = time, error = null)
    }

    fun confirmBooking() {
        val practitioner = _uiState.value.selectedPractitioner ?: return
        val date = _uiState.value.selectedDate ?: return
        val time = _uiState.value.selectedTime ?: return
        _uiState.value = _uiState.value.copy(isBooking = true, error = null)
        viewModelScope.launch {
            try {
                val scheduledAt = "${date}T${time}:00Z"
                val practId = practitioner.id ?: practitioner.userId
                api.bookAppointment(
                    BookAppointmentRequest(
                        practitionerProfileId = practId,
                        scheduledAt = scheduledAt,
                    )
                )
                _uiState.value = _uiState.value.copy(isBooking = false, bookingConfirmed = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isBooking = false,
                    error = "Erreur lors de la réservation: ${e.message}",
                )
            }
        }
    }
}
