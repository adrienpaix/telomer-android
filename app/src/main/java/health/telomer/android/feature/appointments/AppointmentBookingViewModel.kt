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
import javax.inject.Inject

data class BookingUiState(
    val isLoading: Boolean = true,
    val practitioners: List<PractitionerResponse> = emptyList(),
    val selectedPractitioner: PractitionerResponse? = null,
    val slots: List<AvailableSlot> = emptyList(),
    val selectedSlot: AvailableSlot? = null,
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
            selectedSlot = null,
            slots = emptyList(),
        )
        viewModelScope.launch {
            try {
                val slots = api.getAvailableSlots(practitioner.userId)
                _uiState.value = _uiState.value.copy(slots = slots)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Impossible de charger les créneaux: ${e.message}")
            }
        }
    }

    fun selectSlot(slot: AvailableSlot) {
        _uiState.value = _uiState.value.copy(selectedSlot = slot, error = null)
    }

    fun confirmBooking() {
        val practitioner = _uiState.value.selectedPractitioner ?: return
        val slot = _uiState.value.selectedSlot ?: return
        _uiState.value = _uiState.value.copy(isBooking = true, error = null)
        viewModelScope.launch {
            try {
                api.bookAppointment(
                    BookAppointmentRequest(
                        practitionerId = practitioner.userId,
                        date = slot.date,
                        time = slot.time,
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
