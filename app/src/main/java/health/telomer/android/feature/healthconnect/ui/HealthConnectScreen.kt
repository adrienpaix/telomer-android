package health.telomer.android.feature.healthconnect.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
        modifier = Modifier.fillMaxSize().background(WhoopDark),
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
    Box(Modifier.fillMaxSize().background(WhoopDark), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("\uD83D\uDE14", fontSize = 64.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                "Health Connect n'est pas supporté\nsur cet appareil",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = WhoopTextSecondary,
            )
        }
    }
}

@Composable
private fun NotInstalledContent(onInstall: () -> Unit) {
    Box(Modifier.fillMaxSize().background(WhoopDark), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("\uD83D\uDCF2", fontSize = 64.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                "Installez Health Connect\ndepuis le Play Store",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = WhoopTextSecondary,
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onInstall,
                colors = ButtonDefaults.buttonColors(containerColor = WhoopBlue),
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
    Box(Modifier.fillMaxSize().background(WhoopDark), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            Text("\uD83D\uDD10", fontSize = 64.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                "Autorisez Telomer Health à lire\nvos données de santé",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = WhoopTextSecondary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Pas · Fréquence cardiaque · Sommeil\nPoids · Composition · SpO2 · Température",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = WhoopTextSecondary,
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onRequestPermissions,
                colors = ButtonDefaults.buttonColors(containerColor = WhoopBlue),
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

    // Sleep efficiency calculation
    val bedtimeEpoch = lastSleep?.metadata?.get("bedtime_epoch")?.toLong()
    val wakeEpoch = lastSleep?.metadata?.get("wake_epoch")?.toLong()
    val timeInBedMin = if (bedtimeEpoch != null && wakeEpoch != null) {
        ((wakeEpoch - bedtimeEpoch) / 60.0).roundToInt()
    } else sleepMin // fallback
    val sleepEfficiency = if (sleepMin > 0 && timeInBedMin > 0) {
        ((sleepMin.toFloat() / timeInBedMin) * 100).roundToInt()
    } else null

    val sleepScore = computeSleepScore(sleepMin, sleepDeep, sleepRem, sleepLight)
    val recoveryScore = computeRecoveryScore(hrResting, lastHRV?.value, lastSpO2?.value, sleepScore)
    val strainScore = computeStrainScore(steps, activeCals, exerciseMin, zoneMinutes)
    val sleepDebtHours = computeSleepDebt(sleepMin)
    val dateText = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRANCE))
        .replaceFirstChar { it.uppercase() }

    // Yesterday metrics for trends
    val yesterdaySteps = state.yesterdayMetrics[MetricType.STEPS]?.sumOf { it.value }?.roundToInt()
    val yesterdayActiveCals = state.yesterdayMetrics[MetricType.ACTIVE_CALORIES]?.sumOf { it.value }?.roundToInt()
    val yesterdayExerciseMin = state.yesterdayMetrics[MetricType.EXERCISE]?.sumOf { it.value }?.roundToInt()
    val yesterdayHrResting = state.yesterdayMetrics[MetricType.RESTING_HEART_RATE]?.lastOrNull()?.value?.roundToInt()
    val yesterdayHRV = state.yesterdayMetrics[MetricType.HRV]?.lastOrNull()?.value?.roundToInt()
    val yesterdaySpO2 = state.yesterdayMetrics[MetricType.SPO2]?.lastOrNull()?.value?.let { String.format(Locale.FRANCE, "%.1f", it) }
    val yesterdayWeight = state.yesterdayMetrics[MetricType.WEIGHT]?.lastOrNull()?.value?.let { String.format(Locale.FRANCE, "%.1f", it) }

    // Build weekly strain/recovery for chart
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now()
    val allWeekMetrics = state.weekMetrics.values.flatten()
    val allByDay = allWeekMetrics.groupBy { it.recordedAt.atZone(zone).toLocalDate() }
        .mapValues { (_, metrics) -> metrics.groupBy { it.type } }

    val strainWeek = (6 downTo 0).map { daysAgo ->
        val date = today.minusDays(daysAgo.toLong())
        val dayMetrics = allByDay[date] ?: emptyMap()
        val s = dayMetrics[MetricType.STEPS]?.sumOf { it.value }?.toInt() ?: 0
        val c = dayMetrics[MetricType.ACTIVE_CALORIES]?.sumOf { it.value }?.toInt() ?: 0
        val e = dayMetrics[MetricType.EXERCISE]?.sumOf { it.value }?.toInt() ?: 0
        val zMins = listOf(
            MetricType.HEART_ZONE_1, MetricType.HEART_ZONE_2,
            MetricType.HEART_ZONE_3, MetricType.HEART_ZONE_4, MetricType.HEART_ZONE_5,
        ).map { zType -> dayMetrics[zType]?.sumOf { it.value }?.toInt() ?: 0 }
        computeStrainScore(s, c, e, zMins)
    }

    val recoveryWeek = (6 downTo 0).map { daysAgo ->
        val date = today.minusDays(daysAgo.toLong())
        val dayMetrics = allByDay[date] ?: emptyMap()
        val dayHrResting = dayMetrics[MetricType.RESTING_HEART_RATE]?.lastOrNull()?.value?.roundToInt()
        val dayHRV = dayMetrics[MetricType.HRV]?.lastOrNull()?.value
        val daySpO2 = dayMetrics[MetricType.SPO2]?.lastOrNull()?.value
        val daySleepMin = dayMetrics[MetricType.SLEEP]?.sumOf { it.value }?.roundToInt() ?: 0
        val daySleepData = dayMetrics[MetricType.SLEEP]?.lastOrNull()
        val dayDeep = daySleepData?.metadata?.get("deep_minutes")?.roundToInt() ?: 0
        val dayRem = daySleepData?.metadata?.get("rem_minutes")?.roundToInt() ?: 0
        val dayLight = daySleepData?.metadata?.get("light_minutes")?.roundToInt() ?: 0
        val daySleepScore = computeSleepScore(daySleepMin, dayDeep, dayRem, dayLight)
        computeRecoveryScore(dayHrResting, dayHRV, daySpO2, daySleepScore)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(WhoopDark),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item {
            DashboardHeader(
                onBack = onBack,
                dateText = dateText,
                sleepScore = sleepScore,
                recoveryScore = recoveryScore,
                strainScore = strainScore,
                sleepDebtHours = sleepDebtHours,
            )
        }

        // Strain/Recovery chart
        if (strainWeek.any { it > 0.0 } || recoveryWeek.any { it > 0 }) {
            item {
                StrainRecoveryChart(
                    strainWeek = strainWeek,
                    recoveryWeek = recoveryWeek,
                    labels = weekDayLabels(),
                )
            }
        }

        state.error?.let { error ->
            item {
                Card(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = WhoopRed.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(error, modifier = Modifier.padding(16.dp), color = WhoopRed, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // Composition corporelle
        item { WhoopCategoryHeader(emoji = "\u2696\uFE0F", title = "Composition corporelle", color = WhoopBlue) }
        item {
            val weightVal = lastWeight?.let { String.format(Locale.FRANCE, "%.1f", it.value) } ?: "\u2014"
            val weightTrend = computeWeightTrend(state.weekMetrics[MetricType.WEIGHT] ?: emptyList())
            WhoopMetricCard(emoji = "\uD83D\uDCCA", label = "Poids", value = weightVal, unit = "kg", progress = 0f, progressColor = WhoopBlue, target = weightTrend,
                previousValue = yesterdayWeight, currentNumeric = lastWeight?.value, previousNumeric = state.yesterdayMetrics[MetricType.WEIGHT]?.lastOrNull()?.value, higherIsBetter = false)
        }
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                WhoopSmallMetricCard(label = "Masse grasse", value = lastBodyFat?.let { String.format(Locale.FRANCE, "%.1f", it.value) } ?: "\u2014", unit = "%", color = WhoopBlue, modifier = Modifier.weight(1f))
                WhoopSmallMetricCard(label = "Masse maigre", value = leanMass?.let { String.format(Locale.FRANCE, "%.1f", it) } ?: "\u2014", unit = "kg", color = WhoopBlue, modifier = Modifier.weight(1f))
            }
        }
        if (lastBodyWater != null) {
            item { WhoopMetricCard(emoji = "\uD83D\uDCA7", label = "Eau corporelle", value = String.format(Locale.FRANCE, "%.1f", lastBodyWater.value), unit = "%") }
        }
        item {
            val weightWeek = buildWeekValues(state.weekMetrics[MetricType.WEIGHT] ?: emptyList()) { metrics -> metrics.lastOrNull()?.value ?: 0.0 }
            if (weightWeek.any { it > 0.0 }) {
                WeekLineChart(values = weightWeek, color = WhoopBlue, labels = weekDayLabels(), title = "Poids \u2014 7 jours", unit = "kg")
            }
        }

        // Activité
        item { WhoopCategoryHeader(emoji = "\uD83C\uDFC3", title = "Activité", color = WhoopGreen) }
        item {
            WhoopMetricCard(
                emoji = "\uD83D\uDC5F",
                label = "Pas",
                value = if (steps > 0) formatFrench(steps) else "\u2014",
                unit = "pas",
                progress = (steps / 10_000f).coerceIn(0f, 1f),
                progressColor = WhoopGreen,
                target = "/ 10 000",
                status = stepsStatus(steps),
                statusColor = stepsStatusColor(steps),
                previousValue = yesterdaySteps?.let { formatFrench(it) },
                currentNumeric = steps.toDouble(),
                previousNumeric = yesterdaySteps?.toDouble(),
                higherIsBetter = true,
            )
        }
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                WhoopSmallMetricCard(label = "Calories actives", value = if (activeCals > 0) formatFrench(activeCals) else "\u2014", unit = "kcal", color = WhoopGreen, modifier = Modifier.weight(1f))
                WhoopSmallMetricCard(label = "Exercice", value = if (exerciseMin > 0) exerciseMin.toString() else "\u2014", unit = "min", color = WhoopGreen, modifier = Modifier.weight(1f))
            }
        }
        if (zoneMinutes.sum() > 0) {
            item { HeartZonesCard(zoneMinutes = zoneMinutes) }
        }

        // Sommeil
        item { WhoopCategoryHeader(emoji = "\uD83D\uDE34", title = "Sommeil", color = WhoopPurple) }
        item {
            val sleepH = sleepMin / 60; val sleepM = sleepMin % 60
            WhoopMetricCard(
                emoji = "\uD83C\uDF19",
                label = "Durée totale",
                value = if (sleepMin > 0) sleepH.toString() + "h " + sleepM.toString() + "m" else "\u2014",
                unit = "",
                progress = (sleepMin / 480f).coerceIn(0f, 1f),
                progressColor = WhoopPurple,
                target = "/ 8h",
                status = sleepStatus(sleepMin),
                statusColor = sleepStatusColor(sleepMin),
            )
        }
        if (sleepLight + sleepDeep + sleepRem > 0) {
            item { SleepStagesCard(lightMin = sleepLight, deepMin = sleepDeep, remMin = sleepRem, bedtime = sleepBedtime, wakeTime = sleepWakeTime) }
        }
        // Sleep efficiency + debt in h:min
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                WhoopSmallMetricCard(
                    label = "Efficacité",
                    value = sleepEfficiency?.let { "$it" } ?: "\u2014",
                    unit = "%",
                    color = WhoopPurple,
                    modifier = Modifier.weight(1f),
                )
                val debtH = sleepDebtHours.toInt()
                val debtM = ((sleepDebtHours - debtH) * 60).roundToInt()
                val debtFormatted = "${debtH}h${String.format("%02d", debtM)}"
                WhoopSmallMetricCard(
                    label = "Dette sommeil",
                    value = if (sleepDebtHours > 0.0) debtFormatted else "0h00",
                    unit = "",
                    color = if (sleepDebtHours > 2.0) WhoopRed else WhoopOrange,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // Cardiovasculaire
        item { WhoopCategoryHeader(emoji = "\u2764\uFE0F", title = "Cardiovasculaire", color = WhoopCyan) }
        item {
            WhoopMetricCard(
                emoji = "\uD83D\uDC93",
                label = "FC au repos",
                value = hrResting?.let { it.toString() } ?: "\u2014",
                unit = "bpm",
                progress = hrResting?.let { hrRestingProgress(it) } ?: 0f,
                progressColor = WhoopRed,
                status = hrResting?.let { hrRestingStatus(it) },
                statusColor = hrResting?.let { hrRestingStatusColor(it) },
                previousValue = yesterdayHrResting?.let { it.toString() },
                currentNumeric = hrResting?.toDouble(),
                previousNumeric = yesterdayHrResting?.toDouble(),
                higherIsBetter = false,
            )
        }
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                WhoopSmallMetricCard(label = "FC moyenne", value = hrAvg?.let { it.toString() } ?: "\u2014", unit = "bpm", color = WhoopRed, modifier = Modifier.weight(1f))
                WhoopSmallMetricCard(label = "FC max", value = hrMax?.let { it.toString() } ?: "\u2014", unit = "bpm", color = WhoopRed, modifier = Modifier.weight(1f))
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                WhoopSmallMetricCard(label = "VFC (HRV)", value = lastHRV?.let { String.format(Locale.FRANCE, "%.0f", it.value) } ?: "\u2014", unit = "ms", color = WhoopCyan, modifier = Modifier.weight(1f))
                WhoopSmallMetricCard(label = "SpO2", value = lastSpO2?.let { String.format(Locale.FRANCE, "%.1f", it.value) } ?: "\u2014", unit = "%", color = WhoopCyan, modifier = Modifier.weight(1f))
            }
        }
        if (lastTemp != null) {
            item { WhoopMetricCard(emoji = "\uD83C\uDF21\uFE0F", label = "Température corporelle", value = String.format(Locale.FRANCE, "%.1f", lastTemp.value), unit = "°C") }
        }
        item {
            val hrWeek = buildWeekValues(state.weekMetrics[MetricType.RESTING_HEART_RATE] ?: emptyList()) { metrics -> metrics.map { it.value }.average() }
            if (hrWeek.any { it > 0.0 }) {
                WeekChart(values = hrWeek, color = WhoopRed, labels = weekDayLabels(), title = "FC repos \u2014 7 jours")
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

// ══════════════════════════════════════════════════════════════════
//  STRAIN / RECOVERY CHART (7 days dual-axis)
// ══════════════════════════════════════════════════════════════════

@Composable
private fun StrainRecoveryChart(
    strainWeek: List<Double>,
    recoveryWeek: List<Int>,
    labels: List<String>,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WhoopCardBg),
        border = BorderStroke(1.dp, WhoopCardBorder),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Effort / Récupération — 7 jours",
                style = MaterialTheme.typography.titleSmall,
                color = WhoopTextPrimary,
            )
            Spacer(Modifier.height(12.dp))
            Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                val barCount = strainWeek.size
                if (barCount == 0) return@Canvas
                val barWidth = size.width / (barCount * 2.5f)
                val spacing = size.width / barCount
                val maxStrain = 21.0
                val maxRecovery = 100

                // Draw strain bars (orange)
                strainWeek.forEachIndexed { i, strain ->
                    val barHeight = (strain / maxStrain * size.height * 0.85f).toFloat()
                    val x = i * spacing + spacing / 2 - barWidth / 2
                    val y = size.height - barHeight
                    drawRoundRect(
                        color = WhoopOrange.copy(alpha = 0.8f),
                        topLeft = Offset(x, y),
                        size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
                    )
                }

                // Draw recovery line (green) with points
                val recoveryPoints = recoveryWeek.mapIndexed { i, rec ->
                    val x = i * spacing + spacing / 2
                    val y = size.height - (rec.toFloat() / maxRecovery * size.height * 0.85f)
                    Offset(x, y)
                }

                // Draw line
                for (i in 1 until recoveryPoints.size) {
                    drawLine(
                        color = WhoopGreen,
                        start = recoveryPoints[i - 1],
                        end = recoveryPoints[i],
                        strokeWidth = 2.5.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }

                // Draw dots
                recoveryPoints.forEach { point ->
                    drawCircle(
                        color = WhoopGreen.copy(alpha = 0.3f),
                        radius = 6.dp.toPx(),
                        center = point,
                    )
                    drawCircle(
                        color = WhoopGreen,
                        radius = 4.dp.toPx(),
                        center = point,
                    )
                    drawCircle(
                        color = WhoopCardBg,
                        radius = 2.dp.toPx(),
                        center = point,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            // Day labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                labels.forEach { l ->
                    Text(l, fontSize = 10.sp, color = WhoopTextSecondary, textAlign = TextAlign.Center)
                }
            }
            Spacer(Modifier.height(8.dp))
            // Legend
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(WhoopOrange, CircleShape))
                Spacer(Modifier.width(4.dp))
                Text("Effort", color = WhoopTextSecondary, style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.width(16.dp))
                Box(Modifier.size(8.dp).background(WhoopGreen, CircleShape))
                Spacer(Modifier.width(4.dp))
                Text("Récupération", color = WhoopTextSecondary, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════
//  DASHBOARD HEADER with GLOW CIRCLES
// ══════════════════════════════════════════════════════════════════

@Composable
private fun DashboardHeader(
    onBack: () -> Unit,
    dateText: String,
    sleepScore: Int,
    recoveryScore: Int,
    strainScore: Double,
    sleepDebtHours: Double,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(colors = listOf(Color(0xFF16213E), WhoopDark)))
            .padding(top = 48.dp, bottom = 24.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour", tint = Color.White)
                }
                Text("Ma Santé", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = Color.White, modifier = Modifier.weight(1f))
            }
            Text(dateText, style = MaterialTheme.typography.bodyMedium, color = WhoopTextSecondary)
            Spacer(Modifier.height(20.dp))
            // 3 glow score circles
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                GlowScoreCircleHC(score = sleepScore, label = "\uD83D\uDE34 Sommeil", color = WhoopPurple, size = 100)
                GlowScoreCircleHC(score = recoveryScore, label = "\uD83D\uDC9A Récupération", color = WhoopGreen, size = 100)
                GlowStrainCircleHC(strain = strainScore, size = 100)
            }
            // Dette de sommeil (formatted h:min)
            if (sleepDebtHours > 0.0) {
                Spacer(Modifier.height(12.dp))
                val debtH = sleepDebtHours.toInt()
                val debtM = ((sleepDebtHours - debtH) * 60).roundToInt()
                val debtFormatted = "${debtH}h${String.format("%02d", debtM)}"
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("\uD83D\uDCA4 ", fontSize = 16.sp)
                    Text(
                        "Dette : $debtFormatted",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (sleepDebtHours > 2.0) WhoopRed else WhoopOrange,
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════
//  GLOW CIRCLES (private to HealthConnectScreen)
// ══════════════════════════════════════════════════════════════════

@Composable
private fun GlowScoreCircleHC(
    score: Int,
    label: String,
    color: Color,
    size: Int = 100,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(size.dp)) {
                val strokeWidth = 10.dp.toPx()
                val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                val fraction = score / 100f
                val sweepAngle = 270f * fraction

                // Glow
                drawArc(
                    color = color.copy(alpha = 0.3f),
                    startAngle = 135f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = strokeWidth + 10.dp.toPx(), cap = StrokeCap.Round),
                )
                // Track
                drawArc(
                    color = WhoopCardBorder,
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = stroke,
                )
                // Progress
                drawArc(
                    color = color,
                    startAngle = 135f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = stroke,
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(score.toString(), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("%", fontSize = 12.sp, color = WhoopTextSecondary)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
private fun GlowStrainCircleHC(
    strain: Double,
    size: Int = 100,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(size.dp)) {
                val strokeWidth = 10.dp.toPx()
                val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                val fraction = (strain / 21.0).coerceIn(0.0, 1.0).toFloat()
                val sweepAngle = 270f * fraction
                val progressColor = when {
                    strain >= 18 -> WhoopRed
                    strain >= 14 -> WhoopOrange
                    strain >= 8 -> WhoopGreen
                    else -> WhoopCyan
                }
                // Glow
                drawArc(color = progressColor.copy(alpha = 0.3f), startAngle = 135f, sweepAngle = sweepAngle, useCenter = false, style = Stroke(width = strokeWidth + 10.dp.toPx(), cap = StrokeCap.Round))
                drawArc(color = WhoopCardBorder, startAngle = 135f, sweepAngle = 270f, useCenter = false, style = stroke)
                drawArc(color = progressColor, startAngle = 135f, sweepAngle = sweepAngle, useCenter = false, style = stroke)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(String.format("%.1f", strain), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("/21", fontSize = 12.sp, color = WhoopTextSecondary)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text("\uD83D\uDD25 Effort", style = MaterialTheme.typography.labelSmall, color = WhoopOrange)
    }
}

// ══════════════════════════════════════════════════════════════════
//  WHOOP-STYLE METRIC CARDS (with trend support)
// ══════════════════════════════════════════════════════════════════

@Composable
private fun WhoopMetricCard(
    emoji: String,
    label: String,
    value: String,
    unit: String,
    target: String? = null,
    status: String? = null,
    statusColor: Color? = null,
    progress: Float = 0f,
    progressColor: Color = WhoopBlue,
    previousValue: String? = null,
    currentNumeric: Double? = null,
    previousNumeric: Double? = null,
    higherIsBetter: Boolean = true,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WhoopCardBg),
        border = BorderStroke(1.dp, WhoopCardBorder),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, fontSize = 20.sp)
                Spacer(Modifier.width(8.dp))
                Text(label, style = MaterialTheme.typography.bodyMedium, color = WhoopTextSecondary)
                Spacer(Modifier.weight(1f))
                status?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = statusColor ?: WhoopTextSecondary) }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = WhoopTextPrimary)
                if (unit.isNotEmpty()) {
                    Spacer(Modifier.width(4.dp))
                    Text(unit, style = MaterialTheme.typography.bodyMedium, color = WhoopTextSecondary, modifier = Modifier.padding(bottom = 4.dp))
                }
                // Trend indicator (J-1)
                if (previousValue != null && currentNumeric != null && previousNumeric != null) {
                    Spacer(Modifier.width(8.dp))
                    val isHigher = currentNumeric > previousNumeric
                    val isEqual = currentNumeric == previousNumeric
                    val arrow = when {
                        isEqual -> "→"
                        isHigher -> "↑"
                        else -> "↓"
                    }
                    val isImprovement = when {
                        isEqual -> true
                        higherIsBetter -> isHigher
                        else -> !isHigher
                    }
                    val arrowColor = if (isImprovement) WhoopGreen else WhoopRed
                    Text(
                        "$arrow $previousValue",
                        style = MaterialTheme.typography.labelSmall,
                        color = arrowColor,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
                if (previousValue == null) {
                    target?.let {
                        Spacer(Modifier.weight(1f))
                        Text(it, style = MaterialTheme.typography.bodySmall, color = WhoopTextSecondary)
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                    target?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = WhoopTextSecondary)
                    }
                }
            }
            if (progress > 0f) {
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().height(3.dp)
                        .background(WhoopCardBorder, RoundedCornerShape(1.5.dp))
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).fillMaxHeight()
                            .background(progressColor, RoundedCornerShape(1.5.dp))
                    )
                }
            }
        }
    }
}

@Composable
private fun WhoopCategoryHeader(
    emoji: String,
    title: String,
    color: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.width(3.dp).height(20.dp).background(color, RoundedCornerShape(1.5.dp)))
        Spacer(Modifier.width(12.dp))
        Text(emoji, fontSize = 18.sp)
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = WhoopTextPrimary)
    }
}

@Composable
private fun WhoopSmallMetricCard(
    label: String,
    value: String,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WhoopCardBg),
        border = BorderStroke(1.dp, WhoopCardBorder),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = WhoopTextSecondary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = WhoopTextPrimary)
                Spacer(Modifier.width(2.dp))
                Text(unit, style = MaterialTheme.typography.labelSmall, color = WhoopTextSecondary, modifier = Modifier.padding(bottom = 2.dp))
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════
//  HELPER FUNCTIONS (unchanged logic)
// ══════════════════════════════════════════════════════════════════

private fun computeSleepScore(sleepMin: Int, deepMin: Int, remMin: Int, lightMin: Int): Int {
    if (sleepMin == 0) return 0
    val totalTarget = 480
    val durationScore = ((sleepMin.toFloat() / totalTarget) * 40).coerceAtMost(40f)
    val totalSleep = (deepMin + remMin + lightMin).coerceAtLeast(1)
    val deepPct = deepMin.toFloat() / totalSleep
    val remPct = remMin.toFloat() / totalSleep
    val deepScore = when {
        deepPct in 0.15f..0.25f -> 30f
        deepPct in 0.10f..0.30f -> 20f
        else -> 10f
    }
    val remScore = when {
        remPct in 0.20f..0.25f -> 30f
        remPct in 0.15f..0.30f -> 20f
        else -> 10f
    }
    return (durationScore + deepScore + remScore).roundToInt().coerceIn(0, 100)
}

private fun computeRecoveryScore(hrResting: Int?, hrvMs: Double?, spo2: Double?, sleepScore: Int): Int {
    var score = 0f
    score += when {
        hrResting == null -> 15f
        hrResting <= 55 -> 30f
        hrResting in 56..65 -> 25f
        hrResting in 66..75 -> 18f
        else -> 10f
    }
    score += when {
        hrvMs == null -> 15f
        hrvMs >= 80 -> 30f
        hrvMs >= 50 -> 22f
        hrvMs >= 30 -> 15f
        else -> 8f
    }
    score += when {
        spo2 == null -> 5f
        spo2 >= 97 -> 10f
        spo2 >= 95 -> 7f
        else -> 3f
    }
    score += (sleepScore * 0.3f)
    return score.roundToInt().coerceIn(0, 100)
}

private fun computeStrainScore(steps: Int, activeCals: Int, exerciseMin: Int, zoneMinutes: List<Int>): Double {
    val zoneWeight = if (zoneMinutes.size >= 5) {
        zoneMinutes[0] * 0.1 + zoneMinutes[1] * 0.3 + zoneMinutes[2] * 0.6 + zoneMinutes[3] * 1.0 + zoneMinutes[4] * 1.5
    } else 0.0
    val stepsContrib = (steps / 1000.0).coerceAtMost(5.0)
    val calsContrib = (activeCals / 200.0).coerceAtMost(5.0)
    val exerciseContrib = (exerciseMin / 15.0).coerceAtMost(5.0)
    val zoneContrib = (zoneWeight / 30.0).coerceAtMost(6.0)
    return (stepsContrib + calsContrib + exerciseContrib + zoneContrib).coerceIn(0.0, 21.0)
}

private fun computeSleepDebt(sleepMinToday: Int): Double {
    val targetMin = 480
    val deficit = (targetMin - sleepMinToday).coerceAtLeast(0)
    return deficit / 60.0
}

private fun stepsStatus(steps: Int): String = when { steps >= 10_000 -> "Objectif atteint"; steps >= 6_000 -> "En bonne voie"; else -> "Insuffisant" }
private fun stepsStatusColor(steps: Int) = when { steps >= 10_000 -> WhoopGreen; steps >= 6_000 -> WhoopOrange; else -> WhoopRed }
private fun hrRestingStatus(hr: Int): String = when { hr in 50..65 -> "Optimal"; hr in 66..75 -> "Normal"; hr in 76..85 -> "Élevée"; else -> "Attention" }
private fun hrRestingStatusColor(hr: Int) = when { hr in 50..65 -> WhoopGreen; hr in 66..75 -> WhoopOrange; hr in 76..85 -> WhoopOrange; else -> WhoopRed }
private fun hrRestingProgress(hr: Int): Float = when { hr in 50..65 -> 1.0f; hr in 66..75 -> 0.7f; hr in 76..85 -> 0.5f; else -> 0.3f }
private fun sleepStatus(sleepMin: Int): String = when { sleepMin >= 450 -> "Optimal"; sleepMin >= 360 -> "Suffisant"; else -> "Insuffisant" }
private fun sleepStatusColor(sleepMin: Int) = when { sleepMin >= 450 -> WhoopGreen; sleepMin >= 360 -> WhoopOrange; else -> WhoopRed }

private fun computeWeightTrend(metrics: List<health.telomer.android.feature.healthconnect.domain.HealthMetric>): String {
    if (metrics.size < 2) return ""
    val sorted = metrics.sortedBy { it.recordedAt }
    val first = sorted.first().value; val last = sorted.last().value
    return when { last > first + 0.2 -> "\u2191"; last < first - 0.2 -> "\u2193"; else -> "\u2192" }
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
