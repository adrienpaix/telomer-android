package health.telomer.android.feature.healthconnect.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import health.telomer.android.core.ui.theme.*
import health.telomer.android.feature.healthconnect.data.HealthConnectAvailability
import androidx.health.connect.client.PermissionController
import health.telomer.android.feature.healthconnect.data.HealthConnectManager
import health.telomer.android.feature.healthconnect.domain.HealthMetric
import health.telomer.android.feature.healthconnect.domain.MetricType
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

// ── Whoop-inspired palette ────────────────────────────────────────
private val DarkBg = Color(0xFF1A1A2E)
private val CardBg = Color(0xFF242438)
private val BarTrack = Color(0xFF3A3A4E)
private val TextSecondary = Color(0xFF9CA3AF)
private val ActivityGreen = TelomerGreen
private val CardioRed = TelomerRed
private val SleepPurple = Color(0xFF8B5CF6)
private val CompositionBlue = Color(0xFF3B82F6)
private val TelomerCyan = TelomerBlue
private val ZoneColors = listOf(
    Color(0xFF6B7280), // Z1 grey
    Color(0xFF3B82F6), // Z2 blue
    TelomerGreen, // Z3 green
    TelomerOrange, // Z4 amber
    TelomerRed, // Z5 red
)

private val frenchNumberFormat: NumberFormat = NumberFormat.getNumberInstance(Locale.FRANCE)
private fun formatFrench(n: Int): String = frenchNumberFormat.format(n)

// ── Entry point ───────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
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

    // Lancer automatiquement la demande de permissions dès que Health Connect est disponible
    LaunchedEffect(state.availability, state.permissionsGranted) {
        if (state.availability == HealthConnectAvailability.AVAILABLE && !state.permissionsGranted) {
            permissionLauncher.launch(HealthConnectManager.PERMISSIONS)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
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
                    PermissionContent(
                        onRequestPermissions = {
                            permissionLauncher.launch(HealthConnectManager.PERMISSIONS)
                        },
                    )
                } else {
                    WhoopDashboard(
                        state = state,
                        onSync = { viewModel.syncNow() },
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}

// ── Empty / error states ──────────────────────────────────────────

@Composable
private fun NotSupportedContent(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().background(DarkBg), contentAlignment = Alignment.Center) {
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
private fun NotInstalledContent(modifier: Modifier = Modifier, onInstall: () -> Unit) {
    Box(modifier.fillMaxSize().background(DarkBg), contentAlignment = Alignment.Center) {
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
private fun PermissionContent(modifier: Modifier = Modifier, onRequestPermissions: () -> Unit) {
    Box(modifier.fillMaxSize().background(DarkBg), contentAlignment = Alignment.Center) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WhoopDashboard(
    state: HealthConnectUiState,
    onSync: () -> Unit,
    onBack: () -> Unit,
) {
    // ── Compute aggregated values ─────────────────────────────────
    val steps = state.todayMetrics[MetricType.STEPS]
        ?.sumOf { it.value }?.roundToInt() ?: 0
    val activeCals = state.todayMetrics[MetricType.ACTIVE_CALORIES]
        ?.sumOf { it.value }?.roundToInt() ?: 0
    val exerciseMin = state.todayMetrics[MetricType.EXERCISE]
        ?.sumOf { it.value }?.roundToInt() ?: 0
    val sleepMin = state.todayMetrics[MetricType.SLEEP]
        ?.sumOf { it.value }?.roundToInt() ?: 0
    val hrValues = state.todayMetrics[MetricType.HEART_RATE]?.map { it.value } ?: emptyList()
    val hrResting = state.weekMetrics[MetricType.RESTING_HEART_RATE]
        ?.lastOrNull()?.value?.roundToInt()
        ?: if (hrValues.isNotEmpty()) hrValues.min().roundToInt() else null
    val hrAvg = if (hrValues.isNotEmpty()) hrValues.average().roundToInt() else null
    val hrMax = if (hrValues.isNotEmpty()) hrValues.max().roundToInt() else null

    // Body composition
    val lastWeight = state.weekMetrics[MetricType.WEIGHT]?.lastOrNull()
    val lastBodyFat = state.weekMetrics[MetricType.BODY_FAT]?.lastOrNull()
    val leanMass = if (lastWeight != null && lastBodyFat != null)
        lastWeight.value * (1.0 - lastBodyFat.value / 100.0)
    else null
    val lastBodyWater = state.weekMetrics[MetricType.BODY_WATER]?.lastOrNull()

    // Cardio extras
    val lastHRV = state.weekMetrics[MetricType.HRV]?.lastOrNull()
    val lastSpO2 = state.weekMetrics[MetricType.SPO2]?.lastOrNull()
    val lastTemp = state.weekMetrics[MetricType.BODY_TEMPERATURE]?.lastOrNull()

    // Heart zones (week cumulative)
    val zoneMinutes = listOf(
        MetricType.HEART_ZONE_1, MetricType.HEART_ZONE_2,
        MetricType.HEART_ZONE_3, MetricType.HEART_ZONE_4, MetricType.HEART_ZONE_5,
    ).map { zType -> state.weekMetrics[zType]?.sumOf { it.value }?.roundToInt() ?: 0 }

    // Sleep stages from last night
    val lastSleep = state.weekMetrics[MetricType.SLEEP]?.lastOrNull()
    val sleepLight = lastSleep?.metadata?.get("light_minutes")?.roundToInt() ?: 0
    val sleepDeep = lastSleep?.metadata?.get("deep_minutes")?.roundToInt() ?: 0
    val sleepRem = lastSleep?.metadata?.get("rem_minutes")?.roundToInt() ?: 0
    val sleepBedtime = lastSleep?.metadata?.get("bedtime_epoch")?.let { epochSec ->
        val fmt = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
        fmt.format(Instant.ofEpochSecond(epochSec.toLong()))
    }
    val sleepWakeTime = lastSleep?.metadata?.get("wake_epoch")?.let { epochSec ->
        val fmt = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
        fmt.format(Instant.ofEpochSecond(epochSec.toLong()))
    }

    // Global score
    val globalScore = computeGlobalScore(steps, exerciseMin, sleepMin, hrResting)
    val dateText = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRANCE))
        .replaceFirstChar { it.uppercase() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(DarkBg),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        // ── Header ────────────────────────────────────────────────
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(colors = listOf(Color(0xFF16213E), DarkBg)))
                    .padding(top = 48.dp, bottom = 24.dp),
            ) {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour", tint = Color.White)
                        }
                        Text(
                            "Ma Santé",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Text(dateText, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Spacer(Modifier.height(20.dp))
                    ScoreCircle(score = globalScore, modifier = Modifier.size(160.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Score du jour", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
        }

        // ── Error banner ──────────────────────────────────────────
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

        // ── ⚖️ Composition corporelle ─────────────────────────────
        item { CategoryHeader(icon = "⚖️", title = "Composition corporelle", color = CompositionBlue) }
        item {
            val weightVal = lastWeight?.let { String.format(Locale.FRANCE, "%.1f", it.value) } ?: "—"
            val weightTrend = computeWeightTrend(state.weekMetrics[MetricType.WEIGHT] ?: emptyList())
            MetricCard(
                icon = "📊", label = "Poids", value = weightVal, unit = "kg",
                progress = 0f, color = CompositionBlue, target = weightTrend, status = null, statusColor = null,
            )
        }
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallMetricCard(
                    label = "Masse grasse",
                    value = lastBodyFat?.let { String.format(Locale.FRANCE, "%.1f", it.value) } ?: "—",
                    unit = "%", color = CompositionBlue, modifier = Modifier.weight(1f),
                )
                SmallMetricCard(
                    label = "Masse maigre",
                    value = leanMass?.let { String.format(Locale.FRANCE, "%.1f", it) } ?: "—",
                    unit = "kg", color = CompositionBlue, modifier = Modifier.weight(1f),
                )
            }
        }
        if (lastBodyWater != null) {
            item {
                SmallMetricCardFull(
                    label = "Eau corporelle",
                    value = String.format(Locale.FRANCE, "%.1f", lastBodyWater.value),
                    unit = "%",
                    icon = "💧",
                    color = CompositionBlue,
                )
            }
        }
        // Weight chart
        item {
            val weightWeek = buildWeekValues(state.weekMetrics[MetricType.WEIGHT] ?: emptyList()) { metrics ->
                metrics.lastOrNull()?.value ?: 0.0
            }
            if (weightWeek.any { it > 0.0 }) {
                WeekLineChart(values = weightWeek, color = CompositionBlue, labels = weekDayLabels(), title = "Poids — 7 jours", unit = "kg")
            }
        }

        // ── 🏃 Activité ──────────────────────────────────────────
        item { CategoryHeader(icon = "🏃", title = "Activité", color = ActivityGreen) }
        item {
            MetricCard(
                icon = "👟", label = "Pas", value = if (steps > 0) formatFrench(steps) else "—", unit = "pas",
                progress = (steps / 10_000f).coerceIn(0f, 1f), color = ActivityGreen, target = "/ 10 000",
                status = stepsStatus(steps), statusColor = stepsStatusColor(steps),
            )
        }
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallMetricCard(label = "Calories actives", value = if (activeCals > 0) formatFrench(activeCals) else "—", unit = "kcal", color = ActivityGreen, modifier = Modifier.weight(1f))
                SmallMetricCard(label = "Exercice", value = if (exerciseMin > 0) "$exerciseMin" else "—", unit = "min", color = ActivityGreen, modifier = Modifier.weight(1f))
            }
        }
        // Heart zones
        if (zoneMinutes.sum() > 0) {
            item {
                HeartZonesCard(zoneMinutes = zoneMinutes)
            }
        }

        // ── 😴 Sommeil ───────────────────────────────────────────
        item { CategoryHeader(icon = "😴", title = "Sommeil", color = SleepPurple) }
        item {
            val sleepH = sleepMin / 60
            val sleepM = sleepMin % 60
            MetricCard(
                icon = "🌙", label = "Durée totale",
                value = if (sleepMin > 0) "${sleepH}h ${sleepM}m" else "—", unit = "",
                progress = (sleepMin / 480f).coerceIn(0f, 1f),
                color = SleepPurple, target = "/ 8h",
                status = sleepStatus(sleepMin), statusColor = sleepStatusColor(sleepMin),
            )
        }
        if (sleepLight + sleepDeep + sleepRem > 0) {
            item {
                SleepStagesCard(
                    lightMin = sleepLight,
                    deepMin = sleepDeep,
                    remMin = sleepRem,
                    bedtime = sleepBedtime,
                    wakeTime = sleepWakeTime,
                )
            }
        }

        // ── ❤️ Cardiovasculaire ───────────────────────────────────
        item { CategoryHeader(icon = "❤️", title = "Cardiovasculaire", color = TelomerCyan) }
        item {
            MetricCard(
                icon = "💓", label = "FC au repos",
                value = hrResting?.let { "$it" } ?: "—", unit = "bpm",
                progress = hrResting?.let { hrRestingProgress(it) } ?: 0f,
                color = CardioRed, target = null,
                status = hrResting?.let { hrRestingStatus(it) },
                statusColor = hrResting?.let { hrRestingStatusColor(it) },
            )
        }
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallMetricCard(label = "FC moyenne", value = hrAvg?.let { "$it" } ?: "—", unit = "bpm", color = CardioRed, modifier = Modifier.weight(1f))
                SmallMetricCard(label = "FC max", value = hrMax?.let { "$it" } ?: "—", unit = "bpm", color = CardioRed, modifier = Modifier.weight(1f))
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallMetricCard(
                    label = "VFC (HRV)",
                    value = lastHRV?.let { String.format(Locale.FRANCE, "%.0f", it.value) } ?: "—",
                    unit = "ms", color = TelomerCyan, modifier = Modifier.weight(1f),
                )
                SmallMetricCard(
                    label = "SpO2",
                    value = lastSpO2?.let { String.format(Locale.FRANCE, "%.1f", it.value) } ?: "—",
                    unit = "%", color = TelomerCyan, modifier = Modifier.weight(1f),
                )
            }
        }
        if (lastTemp != null) {
            item {
                SmallMetricCardFull(
                    label = "Température corporelle",
                    value = String.format(Locale.FRANCE, "%.1f", lastTemp.value),
                    unit = "°C",
                    icon = "🌡️",
                    color = TelomerCyan,
                )
            }
        }
        // HR week chart
        item {
            val hrWeek = buildWeekValues(state.weekMetrics[MetricType.RESTING_HEART_RATE] ?: emptyList()) { metrics ->
                metrics.map { it.value }.average()
            }
            if (hrWeek.any { it > 0.0 }) {
                WeekChart(values = hrWeek, color = CardioRed, labels = weekDayLabels(), title = "FC repos — 7 jours")
            }
        }

        // ── Sync button ──────────────────────────────────────────
        item {
            Spacer(Modifier.height(16.dp))
            SyncButton(isSyncing = state.isSyncing, lastSyncEpoch = state.lastSyncEpoch, syncResult = state.syncResult, backendSyncCount = state.backendSyncCount, backendSyncError = state.backendSyncError, onSync = onSync)
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

// ══════════════════════════════════════════════════════════════════
//  NEW CARDS
// ══════════════════════════════════════════════════════════════════

@Composable
private fun HeartZonesCard(zoneMinutes: List<Int>) {
    val zoneLabels = listOf("Z1 Récup.", "Z2 Endurance", "Z3 Aérobie", "Z4 Anaérobie", "Z5 Max")
    val total = zoneMinutes.sum().coerceAtLeast(1)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Zones FC — 7 jours", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Spacer(Modifier.height(12.dp))
            zoneMinutes.forEachIndexed { i, mins ->
                val fraction = mins.toFloat() / total
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    Text(zoneLabels[i], style = MaterialTheme.typography.labelSmall, color = ZoneColors[i], modifier = Modifier.width(90.dp))
                    Box(modifier = Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(BarTrack)) {
                        Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(fraction).clip(RoundedCornerShape(5.dp)).background(ZoneColors[i]))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("${mins}m", style = MaterialTheme.typography.labelSmall, color = TextSecondary, modifier = Modifier.width(32.dp), textAlign = TextAlign.End)
                }
            }
        }
    }
}

@Composable
private fun SleepStagesCard(lightMin: Int, deepMin: Int, remMin: Int, bedtime: String?, wakeTime: String?) {
    val total = (lightMin + deepMin + remMin).coerceAtLeast(1)
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Phases de sommeil", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                if (bedtime != null && wakeTime != null) {
                    Text("$bedtime → $wakeTime", style = MaterialTheme.typography.labelSmall, color = SleepPurple)
                }
            }
            Spacer(Modifier.height(12.dp))
            listOf(
                Triple("Léger", lightMin, Color(0xFF93C5FD)),
                Triple("Profond", deepMin, SleepPurple),
                Triple("REM", remMin, Color(0xFFC4B5FD)),
            ).forEach { (label, mins, color) ->
                val frac = mins.toFloat() / total
                val h = mins / 60; val m = mins % 60
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    Text(label, style = MaterialTheme.typography.labelSmall, color = color, modifier = Modifier.width(60.dp))
                    Box(modifier = Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(BarTrack)) {
                        Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(frac).clip(RoundedCornerShape(5.dp)).background(color))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(if (h > 0) "${h}h${m}m" else "${m}m", style = MaterialTheme.typography.labelSmall, color = TextSecondary, modifier = Modifier.width(42.dp), textAlign = TextAlign.End)
                }
            }
        }
    }
}

@Composable
private fun SmallMetricCardFull(label: String, value: String, unit: String, icon: String, color: Color) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(icon, fontSize = 20.sp)
            Spacer(Modifier.width(12.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, modifier = Modifier.weight(1f))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.width(4.dp))
                Text(unit, fontSize = 11.sp, color = TextSecondary, modifier = Modifier.padding(bottom = 3.dp))
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════
//  REUSABLE COMPONENTS (same as before)
// ══════════════════════════════════════════════════════════════════

@Composable
private fun ScoreCircle(score: Int, modifier: Modifier = Modifier) {
    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            val strokeWidth = 12.dp.toPx()
            val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            val sweepAngle = 270f * (score / 100f).coerceIn(0f, 1f)
            drawArc(color = BarTrack, startAngle = 135f, sweepAngle = 270f, useCenter = false, style = stroke)
            val progressColor = when {
                score >= 80 -> ActivityGreen
                score >= 60 -> TelomerOrange
                score >= 40 -> TelomerOrange
                else -> CardioRed
            }
            drawArc(color = progressColor, startAngle = 135f, sweepAngle = sweepAngle, useCenter = false, style = stroke)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$score", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("%", fontSize = 16.sp, color = TextSecondary)
        }
    }
}

@Composable
private fun CategoryHeader(icon: String, title: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(icon, fontSize = 20.sp)
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = color)
        Spacer(Modifier.weight(1f))
        Box(modifier = Modifier.width(40.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(color.copy(alpha = 0.4f)))
    }
}

@Composable
private fun MetricCard(icon: String, label: String, value: String, unit: String, progress: Float, color: Color, target: String?, status: String?, statusColor: Color?) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(icon, fontSize = 18.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
                status?.let { s ->
                    val pillColor = statusColor ?: color
                    Surface(shape = RoundedCornerShape(12.dp), color = pillColor.copy(alpha = 0.15f)) {
                        Text(s, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = pillColor)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                if (unit.isNotEmpty()) {
                    Spacer(Modifier.width(6.dp))
                    Text(unit, fontSize = 12.sp, color = TextSecondary, modifier = Modifier.padding(bottom = 4.dp))
                }
                target?.let { t ->
                    Spacer(Modifier.width(6.dp))
                    Text(t, fontSize = 12.sp, color = TextSecondary, modifier = Modifier.padding(bottom = 4.dp))
                }
            }
            if (progress > 0f || target != null) {
                Spacer(Modifier.height(10.dp))
                Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(BarTrack)) {
                    Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(fraction = progress).clip(RoundedCornerShape(4.dp)).background(color))
                }
                Spacer(Modifier.height(4.dp))
                Text("${(progress * 100).roundToInt()}%", fontSize = 11.sp, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun SmallMetricCard(label: String, value: String, unit: String, color: Color, modifier: Modifier = Modifier) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = CardBg), modifier = modifier.padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.width(4.dp))
                Text(unit, fontSize = 11.sp, color = TextSecondary, modifier = Modifier.padding(bottom = 3.dp))
            }
        }
    }
}

@Composable
private fun WeekChart(values: List<Double>, color: Color, labels: List<String>, title: String) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = CardBg), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Spacer(Modifier.height(12.dp))
            val maxVal = values.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
            Row(modifier = Modifier.fillMaxWidth().height(80.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                values.forEachIndexed { i, v ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom, modifier = Modifier.weight(1f)) {
                        if (v > 0) { Text("${v.roundToInt()}", fontSize = 9.sp, color = TextSecondary); Spacer(Modifier.height(2.dp)) }
                        val barHeight = if (v > 0) (v / maxVal * 50).coerceAtLeast(4.0) else 0.0
                        if (barHeight > 0) {
                            Canvas(modifier = Modifier.width(20.dp).height(barHeight.dp)) {
                                drawRoundRect(color = color, size = Size(size.width, size.height), cornerRadius = CornerRadius(6f, 6f))
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(labels.getOrElse(i) { "" }, fontSize = 10.sp, color = TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekLineChart(values: List<Double>, color: Color, labels: List<String>, title: String, unit: String) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = CardBg), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Spacer(Modifier.height(12.dp))
            val nonZero = values.filter { it > 0 }
            if (nonZero.isEmpty()) return@Column
            val minVal = nonZero.minOrNull() ?: 0.0
            val maxVal = nonZero.maxOrNull() ?: 1.0
            val range = (maxVal - minVal).coerceAtLeast(0.5)
            Canvas(modifier = Modifier.fillMaxWidth().height(60.dp)) {
                val points = mutableListOf<Offset>()
                val segmentWidth = size.width / (values.size - 1).coerceAtLeast(1)
                values.forEachIndexed { i, v ->
                    if (v > 0) {
                        val x = i * segmentWidth
                        val y = size.height - ((v - minVal) / range * size.height * 0.8f + size.height * 0.1f).toFloat()
                        points.add(Offset(x, y))
                    }
                }
                for (i in 1 until points.size) {
                    drawLine(color = color, start = points[i - 1], end = points[i], strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
                }
                points.forEach { p ->
                    drawCircle(color = color, radius = 5.dp.toPx(), center = p)
                    drawCircle(color = CardBg, radius = 3.dp.toPx(), center = p)
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                labels.forEach { l -> Text(l, fontSize = 10.sp, color = TextSecondary) }
            }
        }
    }
}

@Composable
private fun SyncButton(isSyncing: Boolean, lastSyncEpoch: Long?, syncResult: health.telomer.android.feature.healthconnect.data.SyncResult?, backendSyncCount: Int?, backendSyncError: String?, onSync: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val rotation = if (isSyncing) {
        val infiniteTransition = rememberInfiniteTransition(label = "sync")
        infiniteTransition.animateFloat(initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(animation = tween(1000, easing = LinearEasing)), label = "syncRotation").value
    } else 0f
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Button(
            onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onSync() }, enabled = !isSyncing,
            colors = ButtonDefaults.buttonColors(containerColor = TelomerBlue, disabledContainerColor = TelomerBlue.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(20.dp).rotate(rotation))
            Spacer(Modifier.width(8.dp))
            Text(if (isSyncing) "Synchronisation…" else "Synchroniser", fontSize = 16.sp)
        }
        Spacer(Modifier.height(8.dp))
        lastSyncEpoch?.let { epoch ->
            Text("Dernière sync : ${formatRelativeTime(epoch)}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        syncResult?.let { r ->
            Spacer(Modifier.height(4.dp))
            Text("${r.synced} envoyé(s) · ${r.duplicates} doublon(s)", style = MaterialTheme.typography.bodySmall, color = ActivityGreen)
        }
        backendSyncCount?.let { count ->
            Spacer(Modifier.height(4.dp))
            Text("Synchronisé ✓ ($count métriques envoyées)", style = MaterialTheme.typography.bodySmall, color = ActivityGreen)
        }
        backendSyncError?.let { err ->
            Spacer(Modifier.height(4.dp))
            Text(err, style = MaterialTheme.typography.bodySmall, color = CardioRed)
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
private fun stepsStatusColor(steps: Int): Color = when { steps >= 10_000 -> ActivityGreen; steps >= 6_000 -> TelomerOrange; else -> CardioRed }
private fun hrRestingStatus(hr: Int): String = when { hr in 50..65 -> "Optimal"; hr in 66..75 -> "Normal"; hr in 76..85 -> "Élevée"; else -> "Attention" }
private fun hrRestingStatusColor(hr: Int): Color = when { hr in 50..65 -> ActivityGreen; hr in 66..75 -> TelomerOrange; hr in 76..85 -> TelomerOrange; else -> CardioRed }
private fun hrRestingProgress(hr: Int): Float = when { hr in 50..65 -> 1.0f; hr in 66..75 -> 0.7f; hr in 76..85 -> 0.5f; else -> 0.3f }
private fun sleepStatus(sleepMin: Int): String = when { sleepMin >= 450 -> "Optimal"; sleepMin >= 360 -> "Suffisant"; else -> "Insuffisant" }
private fun sleepStatusColor(sleepMin: Int): Color = when { sleepMin >= 450 -> ActivityGreen; sleepMin >= 360 -> TelomerOrange; else -> CardioRed }
private fun computeWeightTrend(metrics: List<HealthMetric>): String {
    if (metrics.size < 2) return ""
    val sorted = metrics.sortedBy { it.recordedAt }
    val first = sorted.first().value; val last = sorted.last().value
    return when { last > first + 0.2 -> "↑"; last < first - 0.2 -> "↓"; else -> "→" }
}

private fun buildWeekValues(metrics: List<HealthMetric>, aggregate: (List<HealthMetric>) -> Double): List<Double> {
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

private fun formatRelativeTime(epochSec: Long): String {
    val now = Instant.now().epochSecond; val diff = now - epochSec
    return when { diff < 60 -> "à l'instant"; diff < 3600 -> "il y a ${diff / 60} min"; diff < 86400 -> "il y a ${diff / 3600}h"; else -> "il y a ${diff / 86400}j" }
}
