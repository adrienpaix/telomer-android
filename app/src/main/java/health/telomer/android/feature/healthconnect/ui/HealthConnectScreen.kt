package health.telomer.android.feature.healthconnect.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import health.telomer.android.feature.healthconnect.domain.MetricType
import androidx.health.connect.client.PermissionController
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val frenchNumberFormat: NumberFormat = NumberFormat.getNumberInstance(Locale.FRANCE)
private fun formatFrench(n: Int): String = frenchNumberFormat.format(n)

// ── Entry point ───────────────────────────────────────────────────

@Composable
fun HealthConnectScreen(
    navController: NavController,
    viewModel: HealthConnectViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) { granted: Set<String> ->
        viewModel.onPermissionsResult(granted)
    }

    LaunchedEffect(state.availability, state.permissionsGranted) {
        if (state.availability == HealthConnectAvailability.AVAILABLE && !state.permissionsGranted) {
            permissionLauncher.launch(HealthConnectManager.PERMISSIONS)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(DarkBg),
    ) {
        when (state.availability) {
            HealthConnectAvailability.NOT_SUPPORTED -> NotSupportedContent()
            HealthConnectAvailability.NOT_INSTALLED -> {
                NotInstalledContent(
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
                    PermissionContent(onRequestPermissions = { permissionLauncher.launch(HealthConnectManager.PERMISSIONS) })
                } else {
                    WhoopDashboard(state = state, onSync = { viewModel.syncNow() }, onBack = { navController.popBackStack() })
                }
            }
        }
    }
}

// ── Empty / error states ──────────────────────────────────────────

@Composable
private fun NotSupportedContent() {
    Box(Modifier.fillMaxSize().background(DarkBg), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("😔", fontSize = 64.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                "Health Connect n'est pas supporté\nsur cet appareil",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = TextSecondary,
            )
        }
    }
}

@Composable
private fun NotInstalledContent(onInstall: () -> Unit) {
    Box(Modifier.fillMaxSize().background(DarkBg), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📲", fontSize = 64.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                "Installez Health Connect\ndepuis le Play Store",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = TextSecondary,
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onInstall,
                colors = ButtonDefaults.buttonColors(containerColor = TelomerBlue),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Default.Download, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Installer Health Connect")
            }
        }
    }
}

@Composable
private fun PermissionContent(onRequestPermissions: () -> Unit) {
    Box(Modifier.fillMaxSize().background(DarkBg), contentAlignment = Alignment.Center) {
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
                color = TextSecondary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Pas · Fréquence cardiaque · Sommeil\nPoids · Composition · SpO2 · Température",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = TextSecondary,
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onRequestPermissions,
                colors = ButtonDefaults.buttonColors(containerColor = TelomerBlue),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Default.HealthAndSafety, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Connecter Health Connect")
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════
//  WHOOP-INSPIRED DASHBOARD
// ══════════════════════════════════════════════════════════════════

@Composable
private fun WhoopDashboard(
    state: HealthConnectUiState,
    onSync: () -> Unit,
    onBack: () -> Unit,
) {
    val steps = state.todayMetrics[MetricType.STEPS]?.sumOf { it.value }?.roundToInt() ?: 0
    val activeCals = state.todayMetrics[MetricType.ACTIVE_CALORIES]?.sumOf { it.value }?.roundToInt() ?: 0
    val exerciseMin = state.todayMetrics[MetricType.EXERCISE]?.sumOf { it.value }?.roundToInt() ?: 0
    val sleepMin = state.todayMetrics[MetricType.SLEEP]?.sumOf { it.value }?.roundToInt() ?: 0
    val hrValues = state.todayMetrics[MetricType.HEART_RATE]?.map { it.value } ?: emptyList()
    val hrResting = state.weekMetrics[MetricType.RESTING_HEART_RATE]
        ?.lastOrNull()?.value?.roundToInt()
        ?: if (hrValues.isNotEmpty()) hrValues.min().roundToInt() else null
    val hrAvg = if (hrValues.isNotEmpty()) hrValues.average().roundToInt() else null
    val hrMax = if (hrValues.isNotEmpty()) hrValues.max().roundToInt() else null

    val lastWeight = state.weekMetrics[MetricType.WEIGHT]?.lastOrNull()
    val lastBodyFat = state.weekMetrics[MetricType.BODY_FAT]?.lastOrNull()
    val leanMass = if (lastWeight != null && lastBodyFat != null)
        lastWeight.value * (1.0 - lastBodyFat.value / 100.0) else null
    val lastBodyWater = state.weekMetrics[MetricType.BODY_WATER]?.lastOrNull()

    val lastHRV = state.weekMetrics[MetricType.HRV]?.lastOrNull()
    val lastSpO2 = state.weekMetrics[MetricType.SPO2]?.lastOrNull()
    val lastTemp = state.weekMetrics[MetricType.BODY_TEMPERATURE]?.lastOrNull()

    val zoneMinutes = listOf(
        MetricType.HEART_ZONE_1, MetricType.HEART_ZONE_2,
        MetricType.HEART_ZONE_3, MetricType.HEART_ZONE_4, MetricType.HEART_ZONE_5,
    ).map { zType -> state.weekMetrics[zType]?.sumOf { it.value }?.roundToInt() ?: 0 }

    val lastSleep = state.weekMetrics[MetricType.SLEEP]?.lastOrNull()
    val sleepLight = lastSleep?.metadata?.get("light_minutes")?.roundToInt() ?: 0
    val sleepDeep = lastSleep?.metadata?.get("deep_minutes")?.roundToInt() ?: 0
    val sleepRem = lastSleep?.metadata?.get("rem_minutes")?.roundToInt() ?: 0
    val sleepBedtime = lastSleep?.metadata?.get("bedtime_epoch")?.let { epochSec ->
        DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
            .format(Instant.ofEpochSecond(epochSec.toLong()))
    }
    val sleepWakeTime = lastSleep?.metadata?.get("wake_epoch")?.let { epochSec ->
        DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
            .format(Instant.ofEpochSecond(epochSec.toLong()))
    }

    val globalScore = computeGlobalScore(steps, exerciseMin, sleepMin, hrResting)
    val dateText = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRANCE))
        .replaceFirstChar { it.uppercase() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(DarkBg),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item {
            DashboardHeader(onBack = onBack, dateText = dateText, globalScore = globalScore)
        }

        state.error?.let { error ->
            item {
                Card(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = CardioRed.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(error, modifier = Modifier.padding(16.dp), color = CardioRed, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // Composition corporelle
        item { CategoryHeader(icon = "⚖️", title = "Composition corporelle", color = CompositionBlue) }
        item {
            val weightVal = lastWeight?.let { String.format(Locale.FRANCE, "%.1f", it.value) } ?: "—"
            val weightTrend = computeWeightTrend(state.weekMetrics[MetricType.WEIGHT] ?: emptyList())
            MetricCard(icon = "📊", label = "Poids", value = weightVal, unit = "kg", progress = 0f, color = CompositionBlue, target = weightTrend, status = null, statusColor = null)
        }
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallMetricCard(label = "Masse grasse", value = lastBodyFat?.let { String.format(Locale.FRANCE, "%.1f", it.value) } ?: "—", unit = "%", color = CompositionBlue, modifier = Modifier.weight(1f))
                SmallMetricCard(label = "Masse maigre", value = leanMass?.let { String.format(Locale.FRANCE, "%.1f", it) } ?: "—", unit = "kg", color = CompositionBlue, modifier = Modifier.weight(1f))
            }
        }
        if (lastBodyWater != null) {
            item { SmallMetricCardFull(label = "Eau corporelle", value = String.format(Locale.FRANCE, "%.1f", lastBodyWater.value), unit = "%", icon = "💧", color = CompositionBlue) }
        }
        item {
            val weightWeek = buildWeekValues(state.weekMetrics[MetricType.WEIGHT] ?: emptyList()) { metrics -> metrics.lastOrNull()?.value ?: 0.0 }
            if (weightWeek.any { it > 0.0 }) {
                WeekLineChart(values = weightWeek, color = CompositionBlue, labels = weekDayLabels(), title = "Poids — 7 jours", unit = "kg")
            }
        }

        // Activité
        item { CategoryHeader(icon = "🏃", title = "Activité", color = ActivityGreen) }
        item { MetricCard(icon = "👟", label = "Pas", value = if (steps > 0) formatFrench(steps) else "—", unit = "pas", progress = (steps / 10_000f).coerceIn(0f, 1f), color = ActivityGreen, target = "/ 10 000", status = stepsStatus(steps), statusColor = stepsStatusColor(steps)) }
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallMetricCard(label = "Calories actives", value = if (activeCals > 0) formatFrench(activeCals) else "—", unit = "kcal", color = ActivityGreen, modifier = Modifier.weight(1f))
                SmallMetricCard(label = "Exercice", value = if (exerciseMin > 0) "$exerciseMin" else "—", unit = "min", color = ActivityGreen, modifier = Modifier.weight(1f))
            }
        }
        if (zoneMinutes.sum() > 0) {
            item { HeartZonesCard(zoneMinutes = zoneMinutes) }
        }

        // Sommeil
        item { CategoryHeader(icon = "😴", title = "Sommeil", color = SleepPurple) }
        item {
            val sleepH = sleepMin / 60; val sleepM = sleepMin % 60
            MetricCard(icon = "🌙", label = "Durée totale", value = if (sleepMin > 0) "${sleepH}h ${sleepM}m" else "—", unit = "", progress = (sleepMin / 480f).coerceIn(0f, 1f), color = SleepPurple, target = "/ 8h", status = sleepStatus(sleepMin), statusColor = sleepStatusColor(sleepMin))
        }
        if (sleepLight + sleepDeep + sleepRem > 0) {
            item { SleepStagesCard(lightMin = sleepLight, deepMin = sleepDeep, remMin = sleepRem, bedtime = sleepBedtime, wakeTime = sleepWakeTime) }
        }

        // Cardiovasculaire
        item { CategoryHeader(icon = "❤️", title = "Cardiovasculaire", color = TelomerCyan) }
        item { MetricCard(icon = "💓", label = "FC au repos", value = hrResting?.let { "$it" } ?: "—", unit = "bpm", progress = hrResting?.let { hrRestingProgress(it) } ?: 0f, color = CardioRed, target = null, status = hrResting?.let { hrRestingStatus(it) }, statusColor = hrResting?.let { hrRestingStatusColor(it) }) }
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallMetricCard(label = "FC moyenne", value = hrAvg?.let { "$it" } ?: "—", unit = "bpm", color = CardioRed, modifier = Modifier.weight(1f))
                SmallMetricCard(label = "FC max", value = hrMax?.let { "$it" } ?: "—", unit = "bpm", color = CardioRed, modifier = Modifier.weight(1f))
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallMetricCard(label = "VFC (HRV)", value = lastHRV?.let { String.format(Locale.FRANCE, "%.0f", it.value) } ?: "—", unit = "ms", color = TelomerCyan, modifier = Modifier.weight(1f))
                SmallMetricCard(label = "SpO2", value = lastSpO2?.let { String.format(Locale.FRANCE, "%.1f", it.value) } ?: "—", unit = "%", color = TelomerCyan, modifier = Modifier.weight(1f))
            }
        }
        if (lastTemp != null) {
            item { SmallMetricCardFull(label = "Température corporelle", value = String.format(Locale.FRANCE, "%.1f", lastTemp.value), unit = "°C", icon = "🌡️", color = TelomerCyan) }
        }
        item {
            val hrWeek = buildWeekValues(state.weekMetrics[MetricType.RESTING_HEART_RATE] ?: emptyList()) { metrics -> metrics.map { it.value }.average() }
            if (hrWeek.any { it > 0.0 }) {
                WeekChart(values = hrWeek, color = CardioRed, labels = weekDayLabels(), title = "FC repos — 7 jours")
            }
        }

        // Sync
        item {
            Spacer(Modifier.height(16.dp))
            SyncButton(isSyncing = state.isSyncing, lastSyncEpoch = state.lastSyncEpoch, syncResult = state.syncResult, backendSyncCount = state.backendSyncCount, backendSyncError = state.backendSyncError, onSync = onSync)
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun DashboardHeader(onBack: () -> Unit, dateText: String, globalScore: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(colors = listOf(Color(0xFF16213E), DarkBg)))
            .padding(top = 48.dp, bottom = 24.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour", tint = Color.White)
                }
                Text("Ma Santé", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = Color.White, modifier = Modifier.weight(1f))
            }
            Text(dateText, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Spacer(Modifier.height(20.dp))
            ScoreCircle(score = globalScore, modifier = Modifier.size(160.dp))
            Spacer(Modifier.height(8.dp))
            Text("Score du jour", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}

// ══════════════════════════════════════════════════════════════════
//  HELPER FUNCTIONS
// ══════════════════════════════════════════════════════════════════

private fun computeGlobalScore(steps: Int, exerciseMin: Int, sleepMin: Int, hrResting: Int?): Int {
    val stepsScore = ((steps / 10_000f) * 100).coerceAtMost(100f)
    val exerciseScore = ((exerciseMin / 30f) * 100).coerceAtMost(100f)
    val sleepScore = ((sleepMin / 480f) * 100).coerceAtMost(100f)
    val hrScore = when { hrResting == null -> 50f; hrResting in 50..70 -> 100f; hrResting in 71..80 -> 70f; else -> 40f }
    return ((stepsScore + exerciseScore + sleepScore + hrScore) / 4).roundToInt().coerceIn(0, 100)
}

private fun stepsStatus(steps: Int): String = when { steps >= 10_000 -> "Objectif atteint"; steps >= 6_000 -> "En bonne voie"; else -> "Insuffisant" }
private fun stepsStatusColor(steps: Int) = when { steps >= 10_000 -> ActivityGreen; steps >= 6_000 -> TelomerOrange; else -> CardioRed }
private fun hrRestingStatus(hr: Int): String = when { hr in 50..65 -> "Optimal"; hr in 66..75 -> "Normal"; hr in 76..85 -> "Élevée"; else -> "Attention" }
private fun hrRestingStatusColor(hr: Int) = when { hr in 50..65 -> ActivityGreen; hr in 66..75 -> TelomerOrange; hr in 76..85 -> TelomerOrange; else -> CardioRed }
private fun hrRestingProgress(hr: Int): Float = when { hr in 50..65 -> 1.0f; hr in 66..75 -> 0.7f; hr in 76..85 -> 0.5f; else -> 0.3f }
private fun sleepStatus(sleepMin: Int): String = when { sleepMin >= 450 -> "Optimal"; sleepMin >= 360 -> "Suffisant"; else -> "Insuffisant" }
private fun sleepStatusColor(sleepMin: Int) = when { sleepMin >= 450 -> ActivityGreen; sleepMin >= 360 -> TelomerOrange; else -> CardioRed }

private fun computeWeightTrend(metrics: List<health.telomer.android.feature.healthconnect.domain.HealthMetric>): String {
    if (metrics.size < 2) return ""
    val sorted = metrics.sortedBy { it.recordedAt }
    val first = sorted.first().value; val last = sorted.last().value
    return when { last > first + 0.2 -> "↑"; last < first - 0.2 -> "↓"; else -> "→" }
}

private fun buildWeekValues(
    metrics: List<health.telomer.android.feature.healthconnect.domain.HealthMetric>,
    aggregate: (List<health.telomer.android.feature.healthconnect.domain.HealthMetric>) -> Double,
): List<Double> {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now()
    val byDay = metrics.groupBy { it.recordedAt.atZone(zone).toLocalDate() }
    return (6 downTo 0).map { daysAgo ->
        val date = today.minusDays(daysAgo.toLong())
        val dayMetrics = byDay[date]
        if (dayMetrics != null && dayMetrics.isNotEmpty()) aggregate(dayMetrics) else 0.0
    }
}

private fun weekDayLabels(): List<String> {
    val today = LocalDate.now()
    val formatter = DateTimeFormatter.ofPattern("E", Locale.FRANCE)
    return (6 downTo 0).map { daysAgo -> today.minusDays(daysAgo.toLong()).format(formatter).take(1).uppercase() }
}
