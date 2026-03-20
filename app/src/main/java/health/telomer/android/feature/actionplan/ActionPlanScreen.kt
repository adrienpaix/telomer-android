package health.telomer.android.feature.actionplan

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import health.telomer.android.core.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionPlanScreen(
    navController: NavController,
    viewModel: ActionPlanViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mon plan d'action") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TelomerWhite),
            )
        },
        containerColor = TelomerBackground,
    ) { padding ->
        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TelomerBlue)
                }
            }
            state.hasNoPlan -> {
                EmptyPlanView(modifier = Modifier.padding(padding))
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Date selector + progress
                    item {
                        DateSelectorHeader(
                            selectedDate = state.selectedDate,
                            progress = state.progress,
                            completedCount = state.completedCount,
                            totalCount = state.totalCount,
                            onPreviousDay = viewModel::previousDay,
                            onNextDay = viewModel::nextDay,
                        )
                    }

                    // Pillar sections
                    items(state.sections, key = { it.pillar.name }) { section ->
                        PillarSectionCard(
                            section = section,
                            onToggleExpand = { viewModel.toggleSection(section.pillar) },
                            onToggleItem = { globalIndex -> viewModel.toggleItem(globalIndex) },
                        )
                    }

                    // Bottom spacer
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun EmptyPlanView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            Text("🏥", fontSize = 48.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                "Aucun plan d'action",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TelomerGray900,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Votre praticien n'a pas encore validé de plan d'action.\nIl sera disponible après votre prochaine consultation.",
                style = MaterialTheme.typography.bodyMedium,
                color = TelomerGray500,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun DateSelectorHeader(
    selectedDate: LocalDate,
    progress: Float,
    completedCount: Int,
    totalCount: Int,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
) {
    val today = LocalDate.now()
    val isToday = selectedDate == today
    val dateFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH)
    val dateText = if (isToday) "Aujourd'hui" else selectedDate.format(dateFormatter)

    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = TelomerBlue),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Date selector row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onPreviousDay) {
                    Icon(Icons.Default.ChevronLeft, "Jour précédent", tint = TelomerWhite)
                }
                Text(
                    dateText,
                    color = TelomerWhite,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                )
                IconButton(
                    onClick = onNextDay,
                    enabled = !isToday,
                ) {
                    Icon(
                        Icons.Default.ChevronRight, "Jour suivant",
                        tint = if (isToday) TelomerWhite.copy(alpha = 0.3f) else TelomerWhite,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Circular progress
            Box(contentAlignment = Alignment.Center) {
                CircularProgressCanvas(
                    progress = progress,
                    size = 80f,
                    strokeWidth = 8f,
                    trackColor = TelomerWhite.copy(alpha = 0.3f),
                    progressColor = TelomerWhite,
                )
                Text(
                    "${(progress * 100).toInt()}%",
                    color = TelomerWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "$completedCount / $totalCount objectifs complétés",
                color = TelomerWhite.copy(alpha = 0.8f),
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
private fun CircularProgressCanvas(
    progress: Float,
    size: Float,
    strokeWidth: Float,
    trackColor: Color,
    progressColor: Color,
) {
    Canvas(modifier = Modifier.size(size.dp)) {
        val sweepAngle = progress * 360f
        val stroke = Stroke(width = strokeWidth.dp.toPx(), cap = StrokeCap.Round)
        val padding = strokeWidth.dp.toPx() / 2f

        // Track
        drawArc(
            color = trackColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            style = stroke,
            topLeft = Offset(padding, padding),
            size = Size(this.size.width - strokeWidth.dp.toPx(), this.size.height - strokeWidth.dp.toPx()),
        )
        // Progress
        drawArc(
            color = progressColor,
            startAngle = -90f,
            sweepAngle = sweepAngle,
            useCenter = false,
            style = stroke,
            topLeft = Offset(padding, padding),
            size = Size(this.size.width - strokeWidth.dp.toPx(), this.size.height - strokeWidth.dp.toPx()),
        )
    }
}

@Composable
private fun PillarSectionCard(
    section: ActionPlanSection,
    onToggleExpand: () -> Unit,
    onToggleItem: (Int) -> Unit,
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = TelomerWhite),
        modifier = Modifier.animateContentSize(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(section.pillar.emoji, fontSize = 20.sp)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            section.pillar.label,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TelomerGray900,
                        )
                        Text(
                            "${section.completedCount}/${section.totalCount}",
                            fontSize = 12.sp,
                            color = TelomerGray500,
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Mini progress bar
                    LinearProgressIndicator(
                        progress = { section.progress },
                        modifier = Modifier.width(48.dp).height(4.dp),
                        color = section.pillar.color,
                        trackColor = section.pillar.color.copy(alpha = 0.15f),
                        drawStopIndicator = {},
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        if (section.isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = TelomerGray500,
                    )
                }
            }

            // Items
            AnimatedVisibility(visible = section.isExpanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    section.items.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val haptic = LocalHapticFeedback.current
                            Checkbox(
                                checked = item.isChecked,
                                onCheckedChange = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onToggleItem(item.globalIndex)
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = section.pillar.color,
                                    uncheckedColor = TelomerGray500,
                                ),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                item.text,
                                color = if (item.isChecked) TelomerGray500 else TelomerGray900,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        }
    }
}
