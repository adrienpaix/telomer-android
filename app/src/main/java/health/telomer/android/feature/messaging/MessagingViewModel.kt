package health.telomer.android.feature.messaging

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import health.telomer.android.core.data.api.TelomerApi
import health.telomer.android.core.data.api.models.ConversationResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MessagingUiState(
    val isLoading: Boolean = true,
    val conversations: List<ConversationResponse> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class MessagingViewModel @Inject constructor(
    private val api: TelomerApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MessagingUiState())
    val uiState: StateFlow<MessagingUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val conversations = api.getConversations()
                _uiState.value = MessagingUiState(isLoading = false, conversations = conversations)
            } catch (e: Exception) {
                _uiState.value = MessagingUiState(isLoading = false, error = e.message)
            }
        }
    }
}
