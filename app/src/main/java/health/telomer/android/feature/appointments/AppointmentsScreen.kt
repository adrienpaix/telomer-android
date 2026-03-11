package health.telomer.android.feature.appointments

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import health.telomer.android.core.data.api.models.AppointmentResponse
import health.telomer.android.core.ui.theme.*
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentsScreen(
    navController: NavController,
    viewModel: AppointmentsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("appointment_booking") },
                containerColor = TelomerBlue,
                contentColor = TelomerWhite,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Prendre un RDV")
            }
        },
        containerColor = TelomerBackground,
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = { viewModel.load() },
            modifier = Modifier.padding(padding),
        ) {
            if (state.isLoading && state.upcoming.isEmpty() && state.past.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TelomerBlue)
                }
            } else if (state.error != null && state.upcoming.isEmpty() && state.past.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ErrorOutline, null, tint = TelomerRed, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(state.error ?: "", color = TelomerGray500)
                        TextButton(onClick = { viewModel.load() }) { Text("Réessayer") }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (state.upcoming.isNotEmpty()) {
                        item {
                            Text(
                                "À venir",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TelomerGray900,
                            )
                        }
                        items(state.upcoming, key = { it.id }) { appt ->
                            AppointmentCard(
                                appt = appt,
                                isUpcoming = true,
                                onCancel = { viewModel.cancelAppointment(appt.id) },
                                onJoinVideo = { navController.navigate("consultation/${appt.id}") },
                            )
                        }
                    }
                    if (state.past.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Passés",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TelomerGray900,
                            )
                        }
                        items(state.past, key = { it.id }) { appt ->
                            AppointmentCard(appt = appt, isUpcoming = false, onCancel = null, onJoinVideo = null)
                        }
                    }
                    if (state.upcoming.isEmpty() && state.past.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(top = 64.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.EventBusy, null, tint = TelomerGray500, modifier = Modifier.size(48.dp))
                                    Spacer(Modifier.height(8.dp))
                                    Text("Aucun rendez-vous", color = TelomerGray500)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}



private fun formatScheduledAt(isoDate: String): String {
    return try {
        val zdt = ZonedDateTime.parse(isoDate)
        val formatter = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy 'à' HH:mm", Locale.FRENCH)
        zdt.format(formatter).replaceFirstChar { it.uppercase() }
    } catch (_: Exception) {
        isoDate.replace("T", " ").take(16)
    }
}

private val joinableStatuses = setOf("upcoming", "patient_waiting", "in_progress", "confirmed", "à venir")

@Composable
private fun AppointmentCard(
    appt: AppointmentResponse,
    isUpcoming: Boolean,
    onCancel: (() -> Unit)?,
    onJoinVideo: (() -> Unit)?,
) {
    var showCancelDialog by remember { mutableStateOf(false) }
    val canJoin = isUpcoming && appt.status?.lowercase() in joinableStatuses

    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = TelomerWhite),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        formatScheduledAt(appt.scheduledAt),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = TelomerGray900,
                    )
                    appt.practitionerName?.let { name ->
                        Text("Dr $name", color = TelomerGray500, style = MaterialTheme.typography.bodyMedium)
                    }
                    appt.durationMin?.let { dur ->
                        Text("$dur min", color = TelomerGray500, style = MaterialTheme.typography.bodySmall)
                    }
                }
                appt.status?.let { status ->
                    StatusPill(status)
                }
            }
            appt.type?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, color = TelomerBlue, style = MaterialTheme.typography.bodySmall)
            }
            if (canJoin || (isUpcoming && onCancel != null)) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (canJoin && onJoinVideo != null) {
                        Button(
                            onClick = onJoinVideo,
                            colors = ButtonDefaults.buttonColors(containerColor = TelomerGreen),
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.Videocam, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Rejoindre")
                        }
                    }
                    if (isUpcoming && onCancel != null) {
                        TextButton(
                            onClick = { showCancelDialog = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = TelomerRed),
                        ) {
                            Text("Annuler")
                        }
                    }
                }
            }
        }
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Annuler le rendez-vous") },
            text = { Text("Êtes-vous sûr de vouloir annuler ce rendez-vous ?") },
            confirmButton = {
                TextButton(onClick = { showCancelDialog = false; onCancel?.invoke() }) {
                    Text("Confirmer", color = TelomerRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) { Text("Non") }
            },
        )
    }
}

@Composable
private fun StatusPill(status: String) {
    val (label, color) = when (status.lowercase()) {
        "confirmed", "confirmé" -> "Confirmé" to TelomerGreen
        "upcoming", "à venir" -> "À venir" to TelomerBlue
        "cancelled", "annulé" -> "Annulé" to TelomerRed
        "pending", "en attente" -> "En attente" to TelomerOrange
        "completed", "terminé" -> "Terminé" to TelomerGray500
        "patient_waiting" -> "En attente" to TelomerOrange
        "in_progress" -> "En cours" to TelomerGreen
        else -> status to TelomerGray500
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.12f),
    ) {
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}
