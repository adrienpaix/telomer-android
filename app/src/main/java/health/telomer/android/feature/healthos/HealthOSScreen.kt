package health.telomer.android.feature.healthos

import kotlin.math.roundToInt
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import health.telomer.android.core.data.api.models.*
import health.telomer.android.core.ui.theme.*

// ── Pillar configuration ──

data class PillarConfig(val icon: ImageVector, val color: Color, val bgColor: Color)

private val PILLAR_CONFIG = mapOf(
    "cardiovascular"  to PillarConfig(Icons.Default.Favorite, Color(0xFFF43F5E), Color(0xFFFFF1F2)),
    "metabolic"       to PillarConfig(Icons.Default.LocalFireDepartment, TelomerOrange, Color(0xFFFFFBEB)),
    "neuro_emotional" to PillarConfig(Icons.Default.Psychology, Color(0xFF8B5CF6), Color(0xFFF5F3FF)),
    "hormonal_immune" to PillarConfig(Icons.Default.Shield, TelomerGreen, Color(0xFFECFDF5)),
    "physical"        to PillarConfig(Icons.Default.FitnessCenter, Color(0xFF3B82F6), Color(0xFFEFF6FF)),
    "sleep"           to PillarConfig(Icons.Default.Bedtime, Color(0xFF6366F1), Color(0xFFEEF2FF)),
    "cellular_aging"  to PillarConfig(Icons.Default.Biotech, Color(0xFF14B8A6), Color(0xFFF0FDFA)),
    "early_detection" to PillarConfig(Icons.Default.Search, Color(0xFF06B6D4), Color(0xFFECFEFF)),
)

private val DEFAULT_CONFIG = PillarConfig(Icons.Default.Info, TelomerGray500, TelomerGray100)

// ── Score Circle ──

@Composable
fun ScoreCircle(score: Double?, size: Dp = 120.dp, strokeWidth: Dp = 12.dp) {
    val animatedProgress by animateFloatAsState(
        targetValue = ((score ?: 0.0) / 100.0).toFloat(),
        animationSpec = tween(1000),
        label = "scoreProgress",
    )
    val color = when {
        score == null -> TelomerGray500
        score >= 70.0 -> TelomerGreen
        score >= 45.0 -> TelomerAmber
        else -> TelomerRed
    }
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val sw = strokeWidth.toPx()
            drawArc(
                Color(0xFFE5E7EB), 0f, 360f, false,
                style = Stroke(sw, cap = StrokeCap.Round),
            )
            drawArc(
                color, -90f, animatedProgress * 360f, false,
                style = Stroke(sw, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = score?.let { kotlin.math.roundToInt(it).toString() } ?: "\u2014",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = (size.value * 0.3f).sp,
                ),
                fontWeight = FontWeight.Bold,
                color = color,
            )
            Text(
                "/ 100",
                style = MaterialTheme.typography.bodySmall,
                color = TelomerGray500,
            )
        }
    }
}

// ── Status Badge (for metrics) ──

@Composable
private fun StatusBadge(status: String?) {
    val (label, color) = when (status) {
        "optimal" -> "Optimal" to TelomerGreen
        "watch" -> "À surveiller" to TelomerOrange
        "alert" -> "Alerte" to TelomerAmber
        "critical" -> "Critique" to TelomerRed
        else -> (status ?: "—") to TelomerGray500
    }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.12f),
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

// ── Trend indicator ──

@Composable
private fun TrendIcon(trend: String?) {
    val (icon, color) = when (trend) {
        "improving" -> Icons.Default.TrendingUp to TelomerGreen
        "declining" -> Icons.Default.TrendingDown to TelomerRed
        "stable" -> Icons.Default.TrendingFlat to TelomerGray500
        else -> return
    }
    Icon(icon, contentDescription = trend, tint = color, modifier = Modifier.size(16.dp))
}

// ── Pillar Card ──

@Composable
private fun PillarCard(
    pillar: PillarSummaryResponse,
    config: PillarConfig,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = config.bgColor),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(config.color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(config.icon, null, tint = config.color, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(8.dp))
                TrendIcon(pillar.trend)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                pillar.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = TelomerGray900,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = pillar.score?.toString() ?: "\u2014",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = config.color,
                )
                Text(
                    "/100",
                    style = MaterialTheme.typography.bodySmall,
                    color = TelomerGray500,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
        }
    }
}

// ── Inflammation Bar ──

@Composable
private fun InflammationBar(inflammation: InflammationResponse?) {
    if (inflammation == null) return
    val index = inflammation.index ?: return

    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = TelomerWhite),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Whatshot, contentDescription = "Inflammation", tint = TelomerAmber, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Inflammation",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TelomerGray900,
                )
                Spacer(Modifier.weight(1f))
                val levelLabel = when (inflammation.level) {
                    "low" -> "Faible"
                    "moderate" -> "Modéré"
                    "high" -> "Élevé"
                    else -> inflammation.level ?: ""
                }
                val levelColor = when (inflammation.level) {
                    "low" -> TelomerGreen
                    "moderate" -> TelomerAmber
                    "high" -> TelomerRed
                    else -> TelomerGray500
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = levelColor.copy(alpha = 0.12f),
                ) {
                    Text(
                        levelLabel,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        color = levelColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                // Gradient bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(TelomerGreen, Color(0xFFFBBF24), TelomerRed)
                            )
                        )
                )
                // Indicator
                val fraction = (index.coerceIn(0, 100)) / 100f
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .wrapContentWidth(Alignment.End)
                        .offset(y = (-3).dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                index <= 30 -> TelomerGreen
                                index <= 60 -> TelomerAmber
                                else -> TelomerRed
                            }
                        )
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("0", style = MaterialTheme.typography.labelSmall, color = TelomerGray500)
                Text(
                    "Index: $index",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TelomerGray900,
                )
                Text("100", style = MaterialTheme.typography.labelSmall, color = TelomerGray500)
            }
        }
    }
}

// ── Pillar Detail Bottom Sheet ──

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PillarDetailSheet(
    pillarCode: String?,
    pillarDetail: PillarDetailResponse?,
    onDismiss: () -> Unit,
) {
    if (pillarCode == null) return
    val config = PILLAR_CONFIG[pillarCode] ?: DEFAULT_CONFIG
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            if (pillarDetail == null) {
                Box(
                    Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = config.color)
                }
            } else {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(config.color.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(config.icon, null, tint = config.color, modifier = Modifier.size(28.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            pillarDetail.label,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TelomerGray900,
                        )
                    }
                    ScoreCircle(score = pillarDetail.score?.toDouble(), size = 72.dp, strokeWidth = 8.dp)
                }

                Spacer(Modifier.height(20.dp))

                // Metrics
                if (pillarDetail.metrics.isNotEmpty()) {
                    Text(
                        "Biomarqueurs",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TelomerGray900,
                    )
                    Spacer(Modifier.height(10.dp))
                    pillarDetail.metrics.forEach { metric ->
                        MetricRow(metric)
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricRow(metric: MetricResponse) {
    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = TelomerGray100),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    metric.label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TelomerGray900,
                )
                metric.scoredAt?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = TelomerGray500)
                }
            }
            metric.value?.let { value ->
                Text(
                    "$value ${metric.unit ?: ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = TelomerGray900,
                )
                Spacer(Modifier.width(8.dp))
            }
            StatusBadge(metric.status)
        }
    }
}

// ── Main Screen ──

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthOSScreen(
    navController: NavController,
    viewModel: HealthOSViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    // Bottom sheet
    if (state.selectedPillarCode != null) {
        PillarDetailSheet(
            pillarCode = state.selectedPillarCode,
            pillarDetail = state.selectedPillar,
            onDismiss = { viewModel.closePillarDetail() },
        )
    }

    PullToRefreshBox(
        isRefreshing = state.isLoading,
        onRefresh = { viewModel.loadDashboard() },
        modifier = Modifier
            .fillMaxSize()
            .background(TelomerBackground),
    ) {
        when {
            state.isLoading && state.dashboard == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TelomerBlue)
                }
            }
            state.error != null && state.dashboard == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.ErrorOutline, contentDescription = "Erreur de chargement",
                            tint = TelomerRed, modifier = Modifier.size(48.dp),
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            state.error ?: "Erreur de chargement",
                            color = TelomerGray500,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(12.dp))
                        TextButton(onClick = { viewModel.loadDashboard() }) {
                            Text("Réessayer")
                        }
                    }
                }
            }
            else -> {
                val dashboard = state.dashboard ?: return@PullToRefreshBox
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                ) {
                    // ── Hero Section ──
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(TelomerNavy, TelomerDarkSurface)
                                ),
                                RoundedCornerShape(20.dp),
                            )
                            .padding(24.dp),
                    ) {
                        Column {
                            Text(
                                "Mon Bilan Santé",
                                color = Color.White,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(16.dp))
                            if (dashboard.globalScore != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    ScoreCircle(
                                        score = dashboard.globalScore,
                                        size = 100.dp,
                                        strokeWidth = 10.dp,
                                    )
                                    Spacer(Modifier.width(20.dp))
                                    Column {
                                        dashboard.computedAt?.let { ts ->
                                            val date = ts.take(10)
                                            Text(
                                                "Mis à jour le $date",
                                                color = TelomerDarkOnSurface,
                                                style = MaterialTheme.typography.bodySmall,
                                            )
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    "Pas encore de score disponible",
                                    color = TelomerDarkOnSurface,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // ── Inflammation Bar ──
                    InflammationBar(dashboard.inflammation)

                    Spacer(Modifier.height(20.dp))

                    // ── Pillar Grid Title ──
                    Text(
                        "Mes piliers de santé",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TelomerGray900,
                    )
                    Spacer(Modifier.height(12.dp))

                    // ── Pillar Grid (non-scrollable inside scrollable column) ──
                    val pillars = dashboard.pillars
                    val rows = pillars.chunked(2)
                    rows.forEach { rowPillars ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            rowPillars.forEach { pillar ->
                                val config = PILLAR_CONFIG[pillar.code] ?: DEFAULT_CONFIG
                                Box(modifier = Modifier.weight(1f)) {
                                    PillarCard(
                                        pillar = pillar,
                                        config = config,
                                        onClick = { viewModel.selectPillar(pillar.code) },
                                    )
                                }
                            }
                            // Fill remaining space if odd number
                            if (rowPillars.size == 1) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }

                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }
}
