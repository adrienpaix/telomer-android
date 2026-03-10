package health.telomer.android.feature.messaging

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import health.telomer.android.core.data.api.TelomerApi
import health.telomer.android.core.data.api.models.MessageResponse
import health.telomer.android.core.data.api.models.SendMessageRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConversationUiState(
    val isLoading: Boolean = true,
    val messages: List<MessageResponse> = emptyList(),
    val error: String? = null,
    val isSending: Boolean = false,
)

@HiltViewModel
class ConversationViewModel @Inject constructor(
    private val api: TelomerApi,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val conversationId: String = savedStateHandle["conversationId"] ?: ""

    private val _uiState = MutableStateFlow(ConversationUiState())
    val uiState: StateFlow<ConversationUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val messages = api.getConversationMessages(conversationId)
                _uiState.value = ConversationUiState(isLoading = false, messages = messages)
            } catch (e: Exception) {
                _uiState.value = ConversationUiState(isLoading = false, error = e.message)
            }
        }
    }

    fun sendMessage(content: String) {
        if (content.isBlank()) return
        _uiState.value = _uiState.value.copy(isSending = true)
        viewModelScope.launch {
            try {
                api.sendMessage(SendMessageRequest(conversationId = conversationId, content = content))
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSending = false, error = "Erreur d'envoi: ${e.message}")
            }
        }
    }
}
