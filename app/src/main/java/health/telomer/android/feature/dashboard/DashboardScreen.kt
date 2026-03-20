package health.telomer.android.feature.dashboard

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import health.telomer.android.core.ui.theme.*
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
                CircularProgressIndicator(color = WhoopBlue)
            }
        } else if (state.error != null && state.firstName.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = "Erreur", tint = WhoopRed, modifier = Modifier.size(48.dp))
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
                    .verticalScroll(rememberScrollState()),
            ) {
                // ══════════════════════════════════════════════
                //  DARK HERO HEADER (Whoop-inspired)
                // ══════════════════════════════════════════════
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(WhoopDark, Color(0xFF111111), WhoopCardBg)
                            )
                        )
                        .padding(top = 48.dp, bottom = 24.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    ) {
                        // Salutation + date
                        Text(
                            text = "Bonjour " + state.firstName + " \uD83D\uDC4B",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = WhoopTextPrimary,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(today, style = MaterialTheme.typography.bodyMedium, color = WhoopTextSecondary)

                        // 3 Glow Score Circles
                        if (state.sleepScore > 0 || state.recoveryScore > 0 || state.strainScore > 0.0) {
                            Spacer(Modifier.height(24.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                            ) {
                                GlowScoreCircle(
                                    score = state.sleepScore,
                                    label = "Sommeil",
                                    emoji = "\uD83D\uDE34",
                                    color = WhoopPurple,
                                )
                                GlowScoreCircle(
                                    score = state.recoveryScore,
                                    label = "Récup.",
                                    emoji = "\uD83D\uDC9A",
                                    color = WhoopGreen,
                                )
                                GlowStrainCircle(
                                    strain = state.strainScore,
                                )
                            }

                            // Dette de sommeil
                            if (state.sleepDebtHours > 0.0) {
                                Spacer(Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text("\uD83D\uDCA4 ", fontSize = 16.sp)
                                    Text(
                                        "Dette : " + String.format("%.1f", state.sleepDebtHours) + "h",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (state.sleepDebtHours > 2.0) WhoopRed else WhoopOrange,
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        // Carte Bilan Santé (dark premium)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onNavigateToHealthOS),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = WhoopCardBg),
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("\uD83E\uDE7A", fontSize = 24.sp)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "Mon Bilan Santé",
                                        color = WhoopTextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        "Score global",
                                        color = WhoopTextSecondary,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                                val healthScore = state.healthOSScore
                                if (healthScore != null) {
                                    Text(
                                        String.format("%.1f", healthScore),
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            healthScore >= 80 -> WhoopGreen
                                            healthScore >= 60 -> WhoopOrange
                                            else -> WhoopRed
                                        },
                                    )
                                    Spacer(Modifier.width(8.dp))
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = "Voir mon bilan santé", tint = WhoopTextSecondary)
                            }
                        }
                    }
                }

                // ══════════════════════════════════════════════
                //  LIGHT SECTION (cards below)
                // ══════════════════════════════════════════════
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                ) {
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
                                Text("Dr " + name, color = TelomerGray500, style = MaterialTheme.typography.bodyMedium)
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
                        val msgCount = state.unreadMessages
                        Text(
                            text = if (msgCount > 0) msgCount.toString() + " message(s)" else "Aucun nouveau message",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (msgCount > 0) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (msgCount > 0) TelomerBlue else TelomerGray500,
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
                        val labelAndColor = when (qStatus) {
                            "completed" -> Pair("Complété ✓", TelomerGreen)
                            "in_progress" -> Pair("En cours…", TelomerOrange)
                            else -> Pair("À remplir", TelomerBlue)
                        }
                        Text(labelAndColor.first, color = labelAndColor.second, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
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
}

// ══════════════════════════════════════════════════════════════════
//  GLOW SCORE CIRCLES (private to Dashboard)
// ══════════════════════════════════════════════════════════════════

@Composable
private fun GlowScoreCircle(
    score: Int,
    label: String,
    emoji: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(80.dp)) {
                val strokeWidth = 8.dp.toPx()
                val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                val fraction = score / 100f
                val sweepAngle = 270f * fraction

                // Glow effect (shadow arc)
                drawArc(
                    color = color.copy(alpha = 0.3f),
                    startAngle = 135f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = strokeWidth + 8.dp.toPx(), cap = StrokeCap.Round),
                )
                // Background track
                drawArc(
                    color = WhoopCardBorder,
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = stroke,
                )
                // Progress arc
                drawArc(
                    color = color,
                    startAngle = 135f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = stroke,
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(score.toString(), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(emoji + " " + label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
private fun GlowStrainCircle(
    strain: Double,
    modifier: Modifier = Modifier,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(80.dp)) {
                val strokeWidth = 8.dp.toPx()
                val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                val fraction = (strain / 21.0).coerceIn(0.0, 1.0).toFloat()
                val sweepAngle = 270f * fraction
                val color = when {
                    strain >= 18 -> WhoopRed
                    strain >= 14 -> WhoopOrange
                    strain >= 8 -> WhoopGreen
                    else -> WhoopCyan
                }
                // Glow
                drawArc(color = color.copy(alpha = 0.3f), startAngle = 135f, sweepAngle = sweepAngle, useCenter = false, style = Stroke(width = strokeWidth + 8.dp.toPx(), cap = StrokeCap.Round))
                drawArc(color = WhoopCardBorder, startAngle = 135f, sweepAngle = 270f, useCenter = false, style = stroke)
                drawArc(color = color, startAngle = 135f, sweepAngle = sweepAngle, useCenter = false, style = stroke)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(String.format("%.1f", strain), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("/21", fontSize = 10.sp, color = WhoopTextSecondary)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text("\uD83D\uDD25 Effort", style = MaterialTheme.typography.labelSmall, color = WhoopOrange)
    }
}

// ══════════════════════════════════════════════════════════════════
//  LIGHT-MODE CARDS (unchanged logic)
// ══════════════════════════════════════════════════════════════════

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
                textAlign = TextAlign.Center,
            )
        }
    }
}
