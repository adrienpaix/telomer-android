package health.telomer.android.feature.documents

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import health.telomer.android.core.data.api.models.DocumentResponse
import health.telomer.android.core.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsScreen(
    navController: NavController,
    viewModel: DocumentsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.uploadDocument(context, it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mes documents") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TelomerWhite),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { filePickerLauncher.launch("*/*") },
                containerColor = TelomerBlue,
                contentColor = TelomerWhite,
            ) {
                Icon(Icons.Default.Upload, "Ajouter un document")
            }
        },
        containerColor = TelomerBackground,
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = { viewModel.load() },
            modifier = Modifier.padding(padding),
        ) {
            when {
                state.isUploading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = TelomerBlue)
                            Spacer(Modifier.height(12.dp))
                            Text("Envoi en cours…", color = TelomerGray500)
                        }
                    }
                }
                state.isLoading && state.documents.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = TelomerBlue)
                    }
                }
                state.error != null && state.documents.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.ErrorOutline, null, tint = TelomerRed, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(8.dp))
                            Text(state.error ?: "", color = TelomerGray500)
                            TextButton(onClick = { viewModel.load() }) { Text("Réessayer") }
                        }
                    }
                }
                state.documents.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.FolderOpen, null, tint = TelomerGray500, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Aucun document", color = TelomerGray500)
                            Text("Ajoutez un document avec le bouton +", color = TelomerGray500, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.documents, key = { it.id }) { doc ->
                            DocumentCard(doc) {
                                doc.fileUrl?.let { url ->
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
private fun DocumentCard(doc: DocumentResponse, onClick: () -> Unit) {
    val icon = when (doc.fileType?.lowercase()) {
        "pdf" -> Icons.Default.PictureAsPdf
        "jpg", "jpeg", "png", "image" -> Icons.Default.Image
        else -> Icons.Default.InsertDriveFile
    }
    val iconTint = when (doc.fileType?.lowercase()) {
        "pdf" -> TelomerRed
        "jpg", "jpeg", "png", "image" -> TelomerGreen
        else -> TelomerBlue
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = TelomerWhite),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(doc.filename, fontWeight = FontWeight.SemiBold, color = TelomerGray900)
                val date = doc.documentDate ?: doc.uploadedAt
                date?.let {
                    Text(it.take(10), color = TelomerGray500, style = MaterialTheme.typography.bodySmall)
                }
                doc.fileType?.let {
                    Text(it.uppercase(), color = TelomerBlue, style = MaterialTheme.typography.labelSmall)
                }
            }
            Icon(Icons.Default.ChevronRight, null, tint = TelomerGray500)
        }
    }
}
