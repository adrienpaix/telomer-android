package health.telomer.android.feature.healthconnect.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import health.telomer.android.core.ui.theme.*
import kotlin.math.roundToInt

// ── Internal palette (shared within package) ──────────────────────
internal val DarkBg = Color(0xFF1A1A2E)
internal val CardBg = Color(0xFF242438)
internal val BarTrack = Color(0xFF3A3A4E)
internal val TextSecondary = Color(0xFF9CA3AF)
internal val ActivityGreen = TelomerGreen
internal val CardioRed = TelomerRed
internal val SleepPurple = Color(0xFF8B5CF6)
internal val CompositionBlue = Color(0xFF3B82F6)
internal val TelomerCyan = TelomerBlue
internal val ZoneColors = listOf(
    Color(0xFF6B7280), // Z1 grey
    Color(0xFF3B82F6), // Z2 blue
    TelomerGreen,      // Z3 green
    TelomerOrange,     // Z4 amber
    TelomerRed,        // Z5 red
)

@Composable
internal fun ScoreCircle(score: Int, modifier: Modifier = Modifier) {
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
internal fun CategoryHeader(icon: String, title: String, color: Color) {
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
internal fun MetricCard(
    icon: String,
    label: String,
    value: String,
    unit: String,
    progress: Float,
    color: Color,
    target: String?,
    status: String?,
    statusColor: Color?,
) {
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
internal fun SmallMetricCard(label: String, value: String, unit: String, color: Color, modifier: Modifier = Modifier) {
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
internal fun SmallMetricCardFull(label: String, value: String, unit: String, icon: String, color: Color) {
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

@Composable
internal fun HeartZonesCard(zoneMinutes: List<Int>) {
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
internal fun SleepStagesCard(lightMin: Int, deepMin: Int, remMin: Int, bedtime: String?, wakeTime: String?) {
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
internal fun WeekChart(values: List<Double>, color: Color, labels: List<String>, title: String) {
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
internal fun WeekLineChart(values: List<Double>, color: Color, labels: List<String>, title: String, unit: String) {
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
