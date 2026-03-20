package health.telomer.android.feature.prescriptions

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import health.telomer.android.core.data.api.models.PrescriptionResponse
import health.telomer.android.core.ui.components.EmptyState
import health.telomer.android.core.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrescriptionsScreen(
    navController: NavController,
    viewModel: PrescriptionsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mes prescriptions") },
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
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = { viewModel.load() },
            modifier = Modifier.padding(padding),
        ) {
            when {
                state.isLoading && state.prescriptions.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = TelomerBlue)
                    }
                }
                state.error != null && state.prescriptions.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.ErrorOutline, null, tint = TelomerRed, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(8.dp))
                            Text(state.error ?: "", color = TelomerGray500)
                            TextButton(onClick = { viewModel.load() }) { Text("Réessayer") }
                        }
                    }
                }
                state.prescriptions.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.MedicalServices, null, tint = TelomerGray500, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Aucune prescription", color = TelomerGray500)
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.prescriptions, key = { it.id }) { prescription ->
                            PrescriptionCard(prescription) {
                                prescription.pdfUrl?.let { url ->
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    context.startActivity(intent)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrescriptionCard(prescription: PrescriptionResponse, onOpen: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = TelomerWhite),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(Icons.Default.Description, null, tint = TelomerBlue, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    prescription.title ?: "Ordonnance du ${prescription.createdAt.take(10)}",
                    fontWeight = FontWeight.SemiBold,
                    color = TelomerGray900,
                )
                prescription.practitionerName?.let {
                    Text("Dr $it", color = TelomerGray500, style = MaterialTheme.typography.bodySmall)
                }
                prescription.practitionerSpecialty?.let {
                    Text(it, color = TelomerGray500, style = MaterialTheme.typography.labelSmall)
                }
                prescription.content?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, color = TelomerGray900, style = MaterialTheme.typography.bodyMedium, maxLines = 4)
                }
                if (prescription.isSigned == true) {
                    Spacer(Modifier.height(4.dp))
                    Text("✓ Signée", color = TelomerGreen, style = MaterialTheme.typography.labelSmall)
                }
            }
            if (prescription.pdfUrl != null) {
                Icon(Icons.Default.PictureAsPdf, null, tint = TelomerRed)
            }
        }
    }
}
