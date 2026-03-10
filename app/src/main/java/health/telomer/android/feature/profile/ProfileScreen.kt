package health.telomer.android.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import health.telomer.android.BuildConfig
import health.telomer.android.core.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    // Handle logout
    LaunchedEffect(state.loggedOut) {
        if (state.loggedOut) {
            navController.navigate("login") {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    Scaffold(containerColor = TelomerBackground) { padding ->
        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TelomerBlue)
                }
            }
            state.error != null && state.profile == null -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ErrorOutline, null, tint = TelomerRed, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(state.error ?: "", color = TelomerGray500)
                        TextButton(onClick = { viewModel.load() }) { Text("Réessayer") }
                    }
                }
            }
            else -> {
                val profile = state.profile ?: return@Scaffold
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(16.dp))

                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(TelomerBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "${profile.firstName.first()}${profile.lastName.first()}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = TelomerBlue,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "${profile.firstName} ${profile.lastName}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TelomerGray900,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(profile.email, color = TelomerGray500)

                    Spacer(Modifier.height(24.dp))

                    // Edit button
                    if (!state.isEditing) {
                        OutlinedButton(onClick = { viewModel.toggleEdit() }) {
                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Modifier")
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // Info card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        colors = CardDefaults.cardColors(containerColor = TelomerWhite),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            ProfileField("Prénom", profile.firstName)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            ProfileField("Nom", profile.lastName)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            ProfileField("Email", profile.email)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                            if (state.isEditing) {
                                Text("Téléphone", style = MaterialTheme.typography.labelMedium, color = TelomerGray500)
                                Spacer(Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = state.editPhone,
                                    onValueChange = { viewModel.updatePhone(it) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                )
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                Text("Adresse", style = MaterialTheme.typography.labelMedium, color = TelomerGray500)
                                Spacer(Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = state.editAddress,
                                    onValueChange = { viewModel.updateAddress(it) },
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 3,
                                    shape = RoundedCornerShape(8.dp),
                                )
                            } else {
                                ProfileField("Téléphone", profile.phone ?: "—")
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                ProfileField("Adresse", profile.address ?: "—")
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            ProfileField("Date de naissance", profile.dateOfBirth?.take(10) ?: "—")
                            profile.gender?.let {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                ProfileField("Genre", it)
                            }
                        }
                    }

                    if (state.isEditing) {
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = { viewModel.toggleEdit() }, modifier = Modifier.weight(1f)) {
                                Text("Annuler")
                            }
                            Button(
                                onClick = { viewModel.saveProfile() },
                                modifier = Modifier.weight(1f),
                                enabled = !state.isSaving,
                                colors = ButtonDefaults.buttonColors(containerColor = TelomerBlue),
                            ) {
                                if (state.isSaving) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = TelomerWhite)
                                } else {
                                    Text("Sauvegarder")
                                }
                            }
                        }
                    }

                    state.error?.let {
                        Spacer(Modifier.height(12.dp))
                        Text(it, color = TelomerRed, style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(Modifier.height(32.dp))

                    // Logout
                    Button(
                        onClick = { viewModel.logout() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = TelomerRed),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.Default.Logout, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Se déconnecter")
                    }

                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Telomer Health v${BuildConfig.VERSION_NAME}",
                        color = TelomerGray500,
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun ProfileField(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = TelomerGray500)
        Spacer(Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.bodyLarge, color = TelomerGray900)
    }
}
