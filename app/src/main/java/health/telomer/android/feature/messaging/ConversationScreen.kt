package health.telomer.android.feature.messaging

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import health.telomer.android.core.data.api.models.MessageResponse
import health.telomer.android.core.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    navController: NavController,
    viewModel: ConversationViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Scroll to bottom when new messages arrive
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Conversation") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TelomerWhite),
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 3.dp,
                color = TelomerWhite,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Votre message…") },
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4,
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            viewModel.sendMessage(messageText)
                            messageText = ""
                        },
                        enabled = messageText.isNotBlank() && !state.isSending,
                    ) {
                        if (state.isSending) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = TelomerBlue)
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Send, "Envoyer", tint = TelomerBlue)
                        }
                    }
                }
            }
        },
        containerColor = TelomerBackground,
    ) { padding ->
        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TelomerBlue)
                }
            }
            state.error != null && state.messages.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ErrorOutline, null, tint = TelomerRed, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(state.error ?: "", color = TelomerGray500)
                        TextButton(onClick = { viewModel.load() }) { Text("Réessayer") }
                    }
                }
            }
            state.messages.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ChatBubbleOutline, null, tint = TelomerGray500, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Aucun message", color = TelomerGray500)
                        Text("Envoyez le premier message !", color = TelomerGray500, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.messages, key = { it.id }) { msg ->
                        MessageBubble(msg)
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: MessageResponse) {
    // Simple heuristic: if sender name contains "Dr" or is a practitioner, it's received
    val isSent = message.senderName == null || !message.senderName.startsWith("Dr")

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isSent) Alignment.End else Alignment.Start,
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isSent) 16.dp else 4.dp,
                bottomEnd = if (isSent) 4.dp else 16.dp,
            ),
            color = if (isSent) TelomerBlue else TelomerWhite,
            tonalElevation = if (isSent) 0.dp else 1.dp,
        ) {
            Column(modifier = Modifier.padding(12.dp).widthIn(max = 280.dp)) {
                Text(
                    message.content,
                    color = if (isSent) TelomerWhite else TelomerGray900,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    message.createdAt.take(16).replace("T", " "),
                    color = if (isSent) TelomerWhite.copy(alpha = 0.7f) else TelomerGray500,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        message.senderName?.let {
            Text(it, style = MaterialTheme.typography.labelSmall, color = TelomerGray500)
        }
    }
}
