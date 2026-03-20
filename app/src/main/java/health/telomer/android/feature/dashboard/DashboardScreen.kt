package health.telomer.android.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import health.telomer.android.core.ui.theme.*
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.Canvas
import health.telomer.android.feature.healthos.ScoreCircle
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val today = LocalDate.now().format(
        DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH)
    ).replaceFirstChar { it.uppercase() }

    // Memoized navigation lambdas — stable across recompositions
    val onRetry = remember(viewModel) { { viewModel.loadDashboard() } }
    val onNavigateToHealthOS = remember(navController) { { navController.navigate("healthos") } }
    val onNavigateToAppointments = remember(navController) { { navController.navigate("appointments") } }
    val onNavigateToMessages = remember(navController) { { navController.navigate("messages") } }
    val onNavigateToActionPlan = remember(navController) { { navController.navigate("action-plan") } }
    val onNavigateToQuestionnaire = remember(navController) { { navController.navigate("questionnaire") } }
    val onNavigateToBooking = remember(navController) { { navController.navigate("appointment_booking") } }
    val onNavigateToDocuments = remember(navController) { { navController.navigate("documents") } }
    val onNavigateToPrescriptions = remember(navController) { { navController.navigate("prescriptions") } }

    PullToRefreshBox(
        isRefreshing = state.isLoading,
        onRefresh = onRetry,
        modifier = Modifier
            .fillMaxSize()
            .background(TelomerBackground),
    ) {
        if (state.isLoading && state.firstName.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TelomerBlue)
            }
        } else if (state.error != null && state.firstName.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = "Erreur", tint = TelomerRed, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(state.error!!, color = TelomerGray500)
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = onRetry) {
                        Text("Réessayer")
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Bonjour ${state.firstName} 👋",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TelomerGray900,
                )
                Spacer(Modifier.height(4.dp))
                Text(today, style = MaterialTheme.typography.bodyMedium, color = TelomerGray500)

                Spacer(Modifier.height(24.dp))

                // ── Health OS Score Card ──
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onNavigateToHealthOS),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = TelomerNavy),
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ScoreCircle(score = state.healthOSScore, size = 72.dp, strokeWidth = 8.dp)
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Mon Bilan Santé",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "Score global",
                                color = Color(0xFF94A3B8),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = "Voir mon bilan santé", tint = Color(0xFF94A3B8))
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ── Health Connect Scores (Whoop-style) ──
                if (state.sleepScore > 0 || state.recoveryScore > 0 || state.strainScore > 0.0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        DashboardScoreCard(
                            label = "Sommeil",
                            score = state.sleepScore,
                            emoji = "😴",
                            color = Color(0xFF8B5CF6),
                            modifier = Modifier.weight(1f),
                        )
                        DashboardScoreCard(
                            label = "Récupération",
                            score = state.recoveryScore,
                            emoji = "💚",
                            color = TelomerGreen,
                            modifier = Modifier.weight(1f),
                        )
                        DashboardStrainCard(
                            label = "Effort",
                            strain = state.strainScore,
                            emoji = "🔥",
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (state.sleepDebtHours > 0.0) {
                        Spacer(Modifier.height(4.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = TelomerWhite),
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("💤", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text("Dette de sommeil", style = MaterialTheme.typography.bodySmall, color = TelomerGray500)
                                    Text(
                                        "${String.format("%.1f", state.sleepDebtHours)}h",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (state.sleepDebtHours > 2.0) TelomerRed else TelomerOrange,
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // Next appointment card
                DashboardCard(
                    icon = Icons.Default.CalendarMonth,
                    title = "Prochain rendez-vous",
                    onClick = onNavigateToAppointments,
                ) {
                    val appt = state.nextAppointment
                    if (appt != null) {
                        val formatted = try {
                            val zdt = ZonedDateTime.parse(appt.scheduledAt)
                            val formatter = DateTimeFormatter.ofPattern("EEEE d MMMM 'à' HH:mm", Locale.FRENCH)
                            zdt.format(formatter).replaceFirstChar { it.uppercase() }
                        } catch (_: Exception) {
                            appt.scheduledAt.replace("T", " ").take(16)
                        }
                        Text(
                            text = formatted,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = TelomerGray900,
                        )
                        appt.practitionerName?.let { name ->
                            Text("Dr $name", color = TelomerGray500, style = MaterialTheme.typography.bodyMedium)
                        }
                        appt.type?.let { Text(it, color = TelomerBlue, style = MaterialTheme.typography.bodySmall) }
                    } else {
                        Text("Aucun rendez-vous à venir", color = TelomerGray500, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Unread messages card
                DashboardCard(
                    icon = Icons.Default.Email,
                    title = "Messages non lus",
                    onClick = onNavigateToMessages,
                ) {
                    Text(
                        text = if (state.unreadMessages > 0) "${state.unreadMessages} message(s)" else "Aucun nouveau message",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (state.unreadMessages > 0) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (state.unreadMessages > 0) TelomerBlue else TelomerGray500,
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Action Plan card
                DashboardCard(
                    icon = Icons.Default.Checklist,
                    title = "Mon plan d'action",
                    onClick = onNavigateToActionPlan,
                ) {
                    Text(
                        "Suivez vos objectifs santé",
                        color = TelomerGreen,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Questionnaire card
                DashboardCard(
                    icon = Icons.Default.Assignment,
                    title = "Mon questionnaire",
                    onClick = onNavigateToQuestionnaire,
                ) {
                    val qStatus = state.questionnaireStatus
                    val (label, color) = when (qStatus) {
                        "completed" -> "Complété ✓" to TelomerGreen
                        "in_progress" -> "En cours…" to TelomerOrange
                        else -> "À remplir" to TelomerBlue
                    }
                    Text(label, color = color, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
                }

                Spacer(Modifier.height(28.dp))

                Text(
                    "Accès rapide",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TelomerGray900,
                )
                Spacer(Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickActionButton(
                        icon = Icons.Default.CalendarMonth,
                        label = "Prendre\nRDV",
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToBooking,
                    )
                    QuickActionButton(
                        icon = Icons.Default.Folder,
                        label = "Mes\ndocuments",
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToDocuments,
                    )
                    QuickActionButton(
                        icon = Icons.Default.MedicalServices,
                        label = "Mes\nprescriptions",
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToPrescriptions,
                    )
                }

                Spacer(Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickActionButton(
                        icon = Icons.Default.Checklist,
                        label = "Plan\nd'action",
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToActionPlan,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.weight(1f))
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun DashboardCard(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = TelomerWhite),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(icon, contentDescription = null, tint = TelomerBlue, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TelomerGray900,
                )
                Spacer(Modifier.height(4.dp))
                content()
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TelomerGray500)
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = TelomerWhite),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, contentDescription = null, tint = TelomerBlue, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = TelomerGray900,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}


@Composable
private fun DashboardScoreCard(
    label: String,
    score: Int,
    emoji: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = TelomerWhite),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(emoji, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                "$score",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = when {
                    score >= 80 -> TelomerGreen
                    score >= 60 -> TelomerOrange
                    else -> TelomerRed
                },
            )
            Text(label, style = MaterialTheme.typography.labelSmall, color = TelomerGray500)
        }
    }
}

@Composable
private fun DashboardStrainCard(
    label: String,
    strain: Double,
    emoji: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = TelomerWhite),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(emoji, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                String.format("%.1f", strain),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = when {
                    strain >= 18.0 -> TelomerRed
                    strain >= 14.0 -> TelomerOrange
                    strain >= 8.0 -> TelomerGreen
                    else -> TelomerBlue
                },
            )
            Text("$label /21", style = MaterialTheme.typography.labelSmall, color = TelomerGray500)
        }
    }
}
