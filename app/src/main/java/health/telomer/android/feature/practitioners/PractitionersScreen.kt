package health.telomer.android.feature.practitioners

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import health.telomer.android.core.data.api.models.PractitionerResponse
import health.telomer.android.core.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PractitionersScreen(
    navController: NavController,
    viewModel: PractitionersViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    // Detail view
    if (state.selectedPractitioner != null) {
        PractitionerDetailScreen(
            practitioner = state.selectedPractitioner!!,
            onBack = { viewModel.clearSelection() },
            onBookAppointment = {
                viewModel.clearSelection()
                navController.navigate("appointment_booking")
            },
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nos praticiens") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TelomerWhite),
            )
        },
        containerColor = TelomerBackground,
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = { viewModel.load() },
            modifier = Modifier.padding(padding),
        ) {
            when {
                state.isLoading && state.practitioners.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = TelomerBlue)
                    }
                }
                state.error != null && state.practitioners.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.ErrorOutline, null, tint = TelomerRed, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(8.dp))
                            Text(state.error ?: "", color = TelomerGray500)
                            TextButton(onClick = { viewModel.load() }) { Text("Réessayer") }
                        }
                    }
                }
                state.practitioners.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.People, null, tint = TelomerGray500, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Aucun praticien disponible", color = TelomerGray500)
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.practitioners, key = { it.userId }) { practitioner ->
                            PractitionerCard(practitioner) {
                                viewModel.selectPractitioner(practitioner)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PractitionerCard(practitioner: PractitionerResponse, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = TelomerWhite),
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            if (practitioner.photoUrl != null) {
                AsyncImage(
                    model = practitioner.photoUrl,
                    contentDescription = null,
                    modifier = Modifier.size(52.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier.size(52.dp).clip(CircleShape).background(TelomerBlue.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "${practitioner.firstName.first()}${practitioner.lastName.first()}",
                        color = TelomerBlue,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
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
            Icon(Icons.Default.ChevronRight, null, tint = TelomerGray500)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PractitionerDetailScreen(
    practitioner: PractitionerResponse,
    onBack: () -> Unit,
    onBookAppointment: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dr ${practitioner.firstName} ${practitioner.lastName}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TelomerWhite),
            )
        },
        containerColor = TelomerBackground,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(16.dp))

            if (practitioner.photoUrl != null) {
                AsyncImage(
                    model = practitioner.photoUrl,
                    contentDescription = null,
                    modifier = Modifier.size(100.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier.size(100.dp).clip(CircleShape).background(TelomerBlue.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "${practitioner.firstName.first()}${practitioner.lastName.first()}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TelomerBlue,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "Dr ${practitioner.firstName} ${practitioner.lastName}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TelomerGray900,
            )
            practitioner.specialties?.joinToString(", ")?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, color = TelomerBlue, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(24.dp))

            practitioner.bio?.let { bio ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    colors = CardDefaults.cardColors(containerColor = TelomerWhite),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Biographie", fontWeight = FontWeight.SemiBold, color = TelomerGray900)
                        Spacer(Modifier.height(8.dp))
                        Text(bio, color = TelomerGray500, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            Button(
                onClick = onBookAppointment,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TelomerBlue),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Default.CalendarMonth, null)
                Spacer(Modifier.width(8.dp))
                Text("Prendre un rendez-vous", fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
