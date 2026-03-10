package health.telomer.android.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import health.telomer.android.auth.TokenManager
import health.telomer.android.core.data.api.TelomerApi
import health.telomer.android.core.data.api.models.PatientProfile
import health.telomer.android.core.data.api.models.ProfileUpdateRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = true,
    val profile: PatientProfile? = null,
    val isEditing: Boolean = false,
    val editPhone: String = "",
    val editAddress: String = "",
    val isSaving: Boolean = false,
    val error: String? = null,
    val loggedOut: Boolean = false,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val api: TelomerApi,
    private val tokenManager: TokenManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val profile = api.getMyProfile()
                _uiState.value = ProfileUiState(
                    isLoading = false,
                    profile = profile,
                    editPhone = profile.phone ?: "",
                    editAddress = profile.address ?: "",
                )
            } catch (e: Exception) {
                _uiState.value = ProfileUiState(isLoading = false, error = e.message)
            }
        }
    }

    fun toggleEdit() {
        val current = _uiState.value
        _uiState.value = current.copy(
            isEditing = !current.isEditing,
            editPhone = current.profile?.phone ?: "",
            editAddress = current.profile?.address ?: "",
        )
    }

    fun updatePhone(value: String) { _uiState.value = _uiState.value.copy(editPhone = value) }
    fun updateAddress(value: String) { _uiState.value = _uiState.value.copy(editAddress = value) }

    fun saveProfile() {
        _uiState.value = _uiState.value.copy(isSaving = true, error = null)
        viewModelScope.launch {
            try {
                val updated = api.updateProfile(
                    ProfileUpdateRequest(
                        phone = _uiState.value.editPhone.ifBlank { null },
                        address = _uiState.value.editAddress.ifBlank { null },
                    )
                )
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    isEditing = false,
                    profile = updated,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSaving = false, error = "Erreur de sauvegarde: ${e.message}")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            tokenManager.clear()
            _uiState.value = _uiState.value.copy(loggedOut = true)
        }
    }
}
