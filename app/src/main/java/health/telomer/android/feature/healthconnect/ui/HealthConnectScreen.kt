package health.telomer.android.feature.healthconnect.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import health.telomer.android.core.ui.theme.*
import health.telomer.android.feature.healthconnect.data.HealthConnectAvailability
import health.telomer.android.feature.healthconnect.data.HealthConnectManager
import health.telomer.android.feature.healthconnect.domain.HealthMetric
import health.telomer.android.feature.healthconnect.domain.MetricType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthConnectScreen(
    navController: NavController,
    viewModel: HealthConnectViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = viewModel.let { HealthConnectManager.permissionContract() },
    ) { granted ->
        viewModel.onPermissionsResult(granted)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Health Connect") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TelomerBlue,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
    ) { padding ->
        when (state.availability) {
            HealthConnectAvailability.NOT_SUPPORTED -> {
                NotSupportedContent(Modifier.padding(padding))
            }
            HealthConnectAvailability.NOT_INSTALLED -> {
                NotInstalledContent(
                    modifier = Modifier.padding(padding),
                    onInstall = {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("market://details?id=com.google.android.apps.healthdata")
                            setPackage("com.android.vending")
                        }
                        context.startActivity(intent)
                    },
                )
            }
            HealthConnectAvailability.AVAILABLE -> {
                if (!state.permissionsGranted) {
                    PermissionContent(
                        modifier = Modifier.padding(padding),
                        onRequestPermissions = {
                            permissionLauncher.launch(HealthConnectManager.PERMISSIONS)
                        },
                    )
                } else {
                    DashboardContent(
                        modifier = Modifier.padding(padding),
                        state = state,
                        onSync = { viewModel.syncNow() },
                    )
                }
            }
        }
    }
}

// ── Empty / error states ──────────────────────────────────────────

@Composable
private fun NotSupportedContent(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("😔", fontSize = 64.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                "Health Connect n'est pas supporté\nsur cet appareil",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = TelomerGray500,
            )
        }
    }
}

@Composable
private fun NotInstalledContent(modifier: Modifier = Modifier, onInstall: () -> Unit) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📲", fontSize = 64.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                "Installez Health Connect\ndepuis le Play Store",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = TelomerGray500,
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onInstall,
                colors = ButtonDefaults.buttonColors(containerColor = TelomerBlue),
            ) {
                Icon(Icons.Default.Download, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Installer Health Connect")
            }
        }
    }
}

@Composable
private fun PermissionContent(modifier: Modifier = Modifier, onRequestPermissions: () -> Unit) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            Text("🔐", fontSize = 64.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                "Autorisez Telomer Health à lire\nvos données de santé",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = TelomerGray500,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Pas · Fréquence cardiaque · Sommeil\nPoids · Calories · Exercice",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = TelomerGray500,
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onRequestPermissions,
                colors = ButtonDefaults.buttonColors(containerColor = TelomerBlue),
            ) {
                Icon(Icons.Default.HealthAndSafety, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Connecter Health Connect")
            }
        }
    }
}

// ── Dashboard ─────────────────────────────────────────────────────

@Composable
private fun DashboardContent(
    modifier: Modifier = Modifier,
    state: HealthConnectUiState,
    onSync: () -> Unit,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
    ) {
        // Error banner
        state.error?.let { error ->
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = TelomerRed.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        error,
                        modifier = Modifier.padding(16.dp),
                        color = TelomerRed,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        // ── Steps with circular progress ──
        item {
            val steps = state.todayMetrics[MetricType.STEPS]
                ?.sumOf { it.value }?.roundToInt() ?: 0
            StepsCard(steps = steps, goal = 10_000)
        }

        // ── Heart rate ──
        item {
            val hrValues = state.todayMetrics[MetricType.HEART_RATE]?.map { it.value } ?: emptyList()
            MetricCard(
                icon = "❤️",
                label = "Fréquence cardiaque",
                value = if (hrValues.isNotEmpty()) "${hrValues.average().roundToInt()}" else "—",
                unit = "bpm",
                subtitle = if (hrValues.isNotEmpty())
                    "Min ${hrValues.min().roundToInt()} · Max ${hrValues.max().roundToInt()}" else null,
                achieved = hrValues.isNotEmpty(),
            )
        }

        // ── Sleep ──
        item {
            val sleepMin = state.todayMetrics[MetricType.SLEEP]
                ?.sumOf { it.value }?.roundToInt() ?: 0
            val hours = sleepMin / 60
            val mins = sleepMin % 60
            MetricCard(
                icon = "😴",
                label = "Sommeil",
                value = if (sleepMin > 0) "${hours}h ${mins}m" else "—",
                unit = "",
                achieved = sleepMin >= 420, // 7h
            )
        }

        // ── Weight ──
        item {
            val lastWeight = state.todayMetrics[MetricType.WEIGHT]?.lastOrNull()
            MetricCard(
                icon = "⚖️",
                label = "Poids",
                value = lastWeight?.let { String.format("%.1f", it.value) } ?: "—",
                unit = "kg",
                achieved = lastWeight != null,
            )
        }

        // ── Active calories ──
        item {
            val cals = state.todayMetrics[MetricType.ACTIVE_CALORIES]
                ?.sumOf { it.value }?.roundToInt() ?: 0
            MetricCard(
                icon = "🔥",
                label = "Calories actives",
                value = if (cals > 0) "$cals" else "—",
                unit = "kcal",
                achieved = cals >= 300,
            )
        }

        // ── Exercise ──
        item {
            val exMin = state.todayMetrics[MetricType.EXERCISE]
                ?.sumOf { it.value }?.roundToInt() ?: 0
            MetricCard(
                icon = "🏃",
                label = "Exercice",
                value = if (exMin > 0) "$exMin" else "—",
                unit = "min",
                achieved = exMin >= 30,
            )
        }

        // ── 7-day steps bar chart ──
        item {
            WeeklyStepsChart(weekMetrics = state.weekMetrics[MetricType.STEPS] ?: emptyList())
        }

        // ── Sync button ──
        item {
            SyncButton(
                isSyncing = state.isSyncing,
                lastSyncEpoch = state.lastSyncEpoch,
                syncResult = state.syncResult,
                onSync = onSync,
            )
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

// ── Metric cards ──────────────────────────────────────────────────

@Composable
private fun StepsCard(steps: Int, goal: Int) {
    val progress = (steps.toFloat() / goal).coerceIn(0f, 1f)
    val achieved = steps >= goal

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Circular progress
            Box(contentAlignment = Alignment.Center) {
                val color = if (achieved) TelomerGreen else TelomerBlue
                Canvas(modifier = Modifier.size(80.dp)) {
                    val stroke = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                    drawArc(
                        color = TelomerGray100,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = stroke,
                    )
                    drawArc(
                        color = color,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = stroke,
                    )
                }
                Text(
                    "🚶",
                    fontSize = 28.sp,
                )
            }
            Spacer(Modifier.width(20.dp))
            Column {
                Text("Pas", style = MaterialTheme.typography.titleMedium)
                Text(
                    "$steps",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (achieved) TelomerGreen else TelomerGray900,
                )
                Text(
                    "Objectif : $goal",
                    style = MaterialTheme.typography.bodySmall,
                    color = TelomerGray500,
                )
            }
        }
    }
}

@Composable
private fun MetricCard(
    icon: String,
    label: String,
    value: String,
    unit: String,
    subtitle: String? = null,
    achieved: Boolean = false,
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(icon, fontSize = 32.sp)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyMedium, color = TelomerGray500)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        value,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (achieved) TelomerGreen else TelomerGray900,
                    )
                    if (unit.isNotEmpty()) {
                        Spacer(Modifier.width(4.dp))
                        Text(unit, style = MaterialTheme.typography.bodySmall, color = TelomerGray500)
                    }
                }
                subtitle?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = TelomerGray500)
                }
            }
            if (achieved) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Atteint",
                    tint = TelomerGreen,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

// ── Weekly steps bar chart ────────────────────────────────────────

@Composable
private fun WeeklyStepsChart(weekMetrics: List<HealthMetric>) {
    // Group by day
    val zone = ZoneId.systemDefault()
    val dailySteps = weekMetrics.groupBy { m ->
        m.recordedAt.atZone(zone).toLocalDate()
    }.mapValues { (_, list) -> list.sumOf { it.value }.roundToInt() }
        .toSortedMap()

    if (dailySteps.isEmpty()) return

    val maxSteps = (dailySteps.values.maxOrNull() ?: 1).coerceAtLeast(1)
    val dayFormatter = DateTimeFormatter.ofPattern("EEE")

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Tendance 7 jours — Pas",
                style = MaterialTheme.typography.titleSmall,
                color = TelomerGray500,
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom,
            ) {
                dailySteps.forEach { (date, steps) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            "${steps / 1000}k",
                            style = MaterialTheme.typography.labelSmall,
                            color = TelomerGray500,
                            fontSize = 9.sp,
                        )
                        Spacer(Modifier.height(4.dp))
                        val barHeight = (steps.toFloat() / maxSteps * 80).coerceAtLeast(4f)
                        val barColor = if (steps >= 10_000) TelomerGreen else TelomerBlue
                        Canvas(
                            modifier = Modifier
                                .width(24.dp)
                                .height(barHeight.dp),
                        ) {
                            drawRoundRect(
                                color = barColor,
                                size = Size(size.width, size.height),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            date.format(dayFormatter),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = TelomerGray500,
                        )
                    }
                }
            }
        }
    }
}

// ── Sync button ───────────────────────────────────────────────────

@Composable
private fun SyncButton(
    isSyncing: Boolean,
    lastSyncEpoch: Long?,
    syncResult: health.telomer.android.feature.healthconnect.data.SyncResult?,
    onSync: () -> Unit,
) {
    val rotation = if (isSyncing) {
        val infiniteTransition = rememberInfiniteTransition(label = "sync")
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
            ),
            label = "syncRotation",
        ).value
    } else 0f

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(
                onClick = onSync,
                enabled = !isSyncing,
                colors = ButtonDefaults.buttonColors(containerColor = TelomerBlue),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    Icons.Default.Sync,
                    contentDescription = null,
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(rotation),
                )
                Spacer(Modifier.width(8.dp))
                Text(if (isSyncing) "Synchronisation…" else "Synchroniser")
            }

            lastSyncEpoch?.let { epoch ->
                val formatted = Instant.ofEpochSecond(epoch)
                    .atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("dd/MM à HH:mm"))
                Spacer(Modifier.height(8.dp))
                Text(
                    "Dernière sync : $formatted",
                    style = MaterialTheme.typography.bodySmall,
                    color = TelomerGray500,
                )
            }

            syncResult?.let { r ->
                Spacer(Modifier.height(4.dp))
                Text(
                    "${r.synced} envoyé(s) · ${r.duplicates} doublon(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = TelomerGreen,
                )
            }
        }
    }
}
