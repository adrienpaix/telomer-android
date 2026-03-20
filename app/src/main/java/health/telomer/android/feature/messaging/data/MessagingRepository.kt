package health.telomer.android.feature.messaging.data

import health.telomer.android.core.data.api.TelomerApi
import health.telomer.android.core.data.api.models.ConversationResponse
import health.telomer.android.core.data.api.models.MessageResponse
import health.telomer.android.core.data.api.models.RecipientResponse
import health.telomer.android.core.data.api.models.SendMessageRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessagingRepository @Inject constructor(private val api: TelomerApi) {
    suspend fun getConversations(): List<ConversationResponse> = api.getConversations()
    suspend fun getRecipients(): List<RecipientResponse> = api.getRecipients()
    suspend fun getMessages(userId: String): List<MessageResponse> = api.getMessages(userId)
    suspend fun sendMessage(userId: String, request: SendMessageRequest): MessageResponse =
        api.sendMessage(userId, request)
}
