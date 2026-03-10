package health.telomer.android.feature.appointments

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import health.telomer.android.core.data.api.models.AvailableSlot
import health.telomer.android.core.data.api.models.PractitionerResponse
import health.telomer.android.core.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentBookingScreen(
    navController: NavController,
    viewModel: AppointmentBookingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Prendre un rendez-vous") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TelomerWhite),
            )
        },
        containerColor = TelomerBackground,
    ) { padding ->
        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TelomerBlue)
                }
            }
            state.bookingConfirmed -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CheckCircle, null, tint = TelomerGreen, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("Rendez-vous confirmé !", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("Vous recevrez une confirmation par email.", color = TelomerGray500)
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = { navController.popBackStack() },
                            colors = ButtonDefaults.buttonColors(containerColor = TelomerBlue),
                        ) {
                            Text("Retour")
                        }
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Step 1: Choose practitioner
                    item {
                        Text(
                            "1. Choisir un praticien",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TelomerGray900,
                        )
                    }
                    items(state.practitioners) { practitioner ->
                        PractitionerSelectionCard(
                            practitioner = practitioner,
                            isSelected = state.selectedPractitioner?.userId == practitioner.userId,
                            onClick = { viewModel.selectPractitioner(practitioner) },
                        )
                    }

                    // Step 2: Choose slot
                    if (state.selectedPractitioner != null) {
                        item {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "2. Choisir un créneau",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TelomerGray900,
                            )
                        }
                        if (state.slots.isEmpty()) {
                            item {
                                Text("Aucun créneau disponible", color = TelomerGray500)
                            }
                        } else {
                            // Group slots by date
                            val grouped = state.slots.groupBy { it.date }
                            grouped.forEach { (date, slots) ->
                                item {
                                    Text(
                                        date,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TelomerGray900,
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(slots) { slot ->
                                            SlotChip(
                                                slot = slot,
                                                isSelected = state.selectedSlot == slot,
                                                onClick = { viewModel.selectSlot(slot) },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Step 3: Confirm
                    if (state.selectedSlot != null) {
                        item {
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.confirmBooking() },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                enabled = !state.isBooking,
                                colors = ButtonDefaults.buttonColors(containerColor = TelomerBlue),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                if (state.isBooking) {
                                    CircularProgressIndicator(color = TelomerWhite, modifier = Modifier.size(24.dp))
                                } else {
                                    Text("Confirmer le rendez-vous", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    state.error?.let { error ->
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = TelomerRed.copy(alpha = 0.1f)),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Text(error, modifier = Modifier.padding(12.dp), color = TelomerRed)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PractitionerSelectionCard(
    practitioner: PractitionerResponse,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) TelomerBlue.copy(alpha = 0.08f) else TelomerWhite,
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, TelomerBlue) else null,
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(TelomerBlue.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "${practitioner.firstName.first()}${practitioner.lastName.first()}",
                    color = TelomerBlue,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Dr ${practitioner.firstName} ${practitioner.lastName}",
                    fontWeight = FontWeight.SemiBold,
                    color = TelomerGray900,
                )
                practitioner.specialties?.joinToString(", ")?.let {
                    Text(it, color = TelomerGray500, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (isSelected) {
                Icon(Icons.Default.CheckCircle, null, tint = TelomerBlue)
            }
        }
    }
}

@Composable
private fun SlotChip(slot: AvailableSlot, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) TelomerBlue else TelomerWhite,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) TelomerBlue else TelomerGray100),
    ) {
        Text(
            text = slot.time,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            color = if (isSelected) TelomerWhite else TelomerGray900,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
