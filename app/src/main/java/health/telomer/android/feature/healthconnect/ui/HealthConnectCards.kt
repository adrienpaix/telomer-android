package health.telomer.android.feature.healthconnect.ui

import androidx.compose.foundation.BorderStroke
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
internal val DarkBg = WhoopDark
internal val CardBg = WhoopCardBg
internal val BarTrack = WhoopCardBorder
internal val TextSecondary = WhoopTextSecondary
internal val ActivityGreen = WhoopGreen
internal val CardioRed = WhoopRed
internal val SleepPurple = WhoopPurple
internal val CompositionBlue = WhoopBlue
internal val TelomerCyan = WhoopCyan
internal val ZoneColors = listOf(
    Color(0xFF6B7280), // Z1 grey
    Color(0xFF3B82F6), // Z2 blue
    WhoopGreen,        // Z3 green
    WhoopOrange,       // Z4 amber
    WhoopRed,          // Z5 red
)

@Composable
internal fun ScoreCircle(score: Int, modifier: Modifier = Modifier) {
    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            val strokeWidth = 12.dp.toPx()
            val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            val sweepAngle = 270f * (score / 100f).coerceIn(0f, 1f)
            // Glow
            val progressColor = when {
                score >= 80 -> WhoopGreen
                score >= 60 -> WhoopOrange
                score >= 40 -> WhoopOrange
                else -> WhoopRed
            }
            drawArc(
                color = progressColor.copy(alpha = 0.3f),
                startAngle = 135f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth + 8.dp.toPx(), cap = StrokeCap.Round),
            )
            drawArc(color = WhoopCardBorder, startAngle = 135f, sweepAngle = 270f, useCenter = false, style = stroke)
            drawArc(color = progressColor, startAngle = 135f, sweepAngle = sweepAngle, useCenter = false, style = stroke)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(score.toString(), fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("%", fontSize = 16.sp, color = WhoopTextSecondary)
        }
    }
}

@Composable
internal fun CategoryHeader(icon: String, title: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.width(3.dp).height(20.dp).background(color, RoundedCornerShape(1.5.dp)))
        Spacer(Modifier.width(12.dp))
        Text(icon, fontSize = 18.sp)
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = WhoopTextPrimary)
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
        colors = CardDefaults.cardColors(containerColor = WhoopCardBg),
        border = BorderStroke(1.dp, WhoopCardBorder),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(icon, fontSize = 20.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(label, style = MaterialTheme.typography.bodyMedium, color = WhoopTextSecondary)
                }
                status?.let { s ->
                    val pillColor = statusColor ?: color
                    Text(s, style = MaterialTheme.typography.labelSmall, color = pillColor)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = WhoopTextPrimary)
                if (unit.isNotEmpty()) {
                    Spacer(Modifier.width(4.dp))
                    Text(unit, style = MaterialTheme.typography.bodyMedium, color = WhoopTextSecondary, modifier = Modifier.padding(bottom = 4.dp))
                }
                target?.let { t ->
                    Spacer(Modifier.weight(1f))
                    Text(t, style = MaterialTheme.typography.bodySmall, color = WhoopTextSecondary)
                }
            }
            if (progress > 0f) {
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().height(3.dp)
                        .background(WhoopCardBorder, RoundedCornerShape(1.5.dp))
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(fraction = progress.coerceIn(0f, 1f)).fillMaxHeight()
                            .background(color, RoundedCornerShape(1.5.dp))
                    )
                }
            }
        }
    }
}

@Composable
internal fun SmallMetricCard(label: String, value: String, unit: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WhoopCardBg),
        border = BorderStroke(1.dp, WhoopCardBorder),
        modifier = modifier.padding(vertical = 4.dp),
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

@Composable
internal fun SmallMetricCardFull(label: String, value: String, unit: String, icon: String, color: Color) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WhoopCardBg),
        border = BorderStroke(1.dp, WhoopCardBorder),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(icon, fontSize = 20.sp)
            Spacer(Modifier.width(12.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, color = WhoopTextSecondary, modifier = Modifier.weight(1f))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = WhoopTextPrimary)
                Spacer(Modifier.width(4.dp))
                Text(unit, style = MaterialTheme.typography.labelSmall, color = WhoopTextSecondary, modifier = Modifier.padding(bottom = 2.dp))
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
        colors = CardDefaults.cardColors(containerColor = WhoopCardBg),
        border = BorderStroke(1.dp, WhoopCardBorder),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Zones FC \u2014 7 jours", style = MaterialTheme.typography.bodySmall, color = WhoopTextSecondary)
            Spacer(Modifier.height(12.dp))
            zoneMinutes.forEachIndexed { i, mins ->
                val fraction = mins.toFloat() / total
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    Text(zoneLabels[i], style = MaterialTheme.typography.labelSmall, color = ZoneColors[i], modifier = Modifier.width(90.dp))
                    Box(modifier = Modifier.weight(1f).height(3.dp).background(WhoopCardBorder, RoundedCornerShape(1.5.dp))) {
                        Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(fraction).background(ZoneColors[i], RoundedCornerShape(1.5.dp)))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(mins.toString() + "m", style = MaterialTheme.typography.labelSmall, color = WhoopTextSecondary, modifier = Modifier.width(32.dp), textAlign = TextAlign.End)
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
        colors = CardDefaults.cardColors(containerColor = WhoopCardBg),
        border = BorderStroke(1.dp, WhoopCardBorder),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Phases de sommeil", style = MaterialTheme.typography.bodySmall, color = WhoopTextSecondary)
                if (bedtime != null && wakeTime != null) {
                    Text(bedtime + " \u2192 " + wakeTime, style = MaterialTheme.typography.labelSmall, color = WhoopPurple)
                }
            }
            Spacer(Modifier.height(12.dp))
            listOf(
                Triple("Léger", lightMin, Color(0xFF93C5FD)),
                Triple("Profond", deepMin, WhoopPurple),
                Triple("REM", remMin, Color(0xFFC4B5FD)),
            ).forEach { (label, mins, color) ->
                val frac = mins.toFloat() / total
                val h = mins / 60; val m = mins % 60
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    Text(label, style = MaterialTheme.typography.labelSmall, color = color, modifier = Modifier.width(60.dp))
                    Box(modifier = Modifier.weight(1f).height(3.dp).background(WhoopCardBorder, RoundedCornerShape(1.5.dp))) {
                        Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(frac).background(color, RoundedCornerShape(1.5.dp)))
                    }
                    Spacer(Modifier.width(8.dp))
                    val timeText = if (h > 0) h.toString() + "h" + m.toString() + "m" else m.toString() + "m"
                    Text(timeText, style = MaterialTheme.typography.labelSmall, color = WhoopTextSecondary, modifier = Modifier.width(42.dp), textAlign = TextAlign.End)
                }
            }
        }
    }
}

@Composable
internal fun WeekChart(values: List<Double>, color: Color, labels: List<String>, title: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WhoopCardBg),
        border = BorderStroke(1.dp, WhoopCardBorder),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.bodySmall, color = WhoopTextSecondary)
            Spacer(Modifier.height(12.dp))
            val maxVal = values.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
            Row(modifier = Modifier.fillMaxWidth().height(80.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                values.forEachIndexed { i, v ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom, modifier = Modifier.weight(1f)) {
                        if (v > 0) { Text(v.roundToInt().toString(), fontSize = 9.sp, color = WhoopTextSecondary); Spacer(Modifier.height(2.dp)) }
                        val barHeight = if (v > 0) (v / maxVal * 50).coerceAtLeast(4.0) else 0.0
                        if (barHeight > 0) {
                            Canvas(modifier = Modifier.width(16.dp).height(barHeight.dp)) {
                                drawRoundRect(color = color, size = Size(size.width, size.height), cornerRadius = CornerRadius(8f, 8f))
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(labels.getOrElse(i) { "" }, fontSize = 10.sp, color = WhoopTextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
internal fun WeekLineChart(values: List<Double>, color: Color, labels: List<String>, title: String, unit: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WhoopCardBg),
        border = BorderStroke(1.dp, WhoopCardBorder),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.bodySmall, color = WhoopTextSecondary)
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
                    // Glow dot
                    drawCircle(color = color.copy(alpha = 0.3f), radius = 8.dp.toPx(), center = p)
                    drawCircle(color = color, radius = 5.dp.toPx(), center = p)
                    drawCircle(color = WhoopCardBg, radius = 3.dp.toPx(), center = p)
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                labels.forEach { l -> Text(l, fontSize = 10.sp, color = WhoopTextSecondary) }
            }
        }
    }
}
