package health.telomer.android.feature.nutrition.ui.journal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import health.telomer.android.core.ui.theme.*
import health.telomer.android.feature.nutrition.domain.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionJournalScreen(
    navController: NavController,
    viewModel: NutritionJournalViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val haptic = LocalHapticFeedback.current

    Scaffold(
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                AnimatedVisibility(visible = state.showAddOptions) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 8.dp),
                    ) {
                        SmallFloatingActionButton(
                            onClick = { navController.navigate("nutrition/camera"); viewModel.toggleAddOptions() },
                            containerColor = TelomerBlue,
                        ) { Icon(Icons.Default.CameraAlt, "Photo", tint = Color.White) }
                        SmallFloatingActionButton(
                            onClick = { navController.navigate("nutrition/scanner"); viewModel.toggleAddOptions() },
                            containerColor = TelomerOrange,
                        ) { Icon(Icons.Default.QrCodeScanner, "Scanner", tint = Color.White) }
                        SmallFloatingActionButton(
                            onClick = { navController.navigate("nutrition/search"); viewModel.toggleAddOptions() },
                            containerColor = TelomerGreen,
                        ) { Icon(Icons.Default.Search, "Recherche", tint = Color.White) }
                    }
                }
                FloatingActionButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.toggleAddOptions()
                    },
                    containerColor = TelomerBlue,
                ) {
                    Icon(
                        if (state.showAddOptions) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = "Ajouter",
                        tint = Color.White,
                    )
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
        ) {
            // Date selector
            item {
                DateSelector(
                    label = viewModel.dateLabel,
                    onPrevious = viewModel::previousDay,
                    onNext = viewModel::nextDay,
                    canGoNext = state.selectedDate.isBefore(java.time.LocalDate.now()),
                )
            }

            // Loading / error
            if (state.isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = TelomerBlue)
                    }
                }
            }

            state.error?.let { error ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = TelomerRed.copy(alpha = 0.1f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(error, modifier = Modifier.padding(16.dp), color = TelomerRed)
                    }
                }
            }

            state.summary?.let { summary ->
                // Calorie circle + macros
                item { CalorieSummaryCard(summary) }

                // Goals link
                item {
                    TextButton(
                        onClick = { navController.navigate("nutrition/goals") },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.TrackChanges, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Modifier mes objectifs")
                    }
                }

                // Meals by type
                val mealsByType = MealType.entries.map { type ->
                    type to summary.meals.filter { it.mealType == type }
                }

                for ((mealType, meals) in mealsByType) {
                    item {
                        MealTypeHeader(mealType, meals.sumOf { it.totalCalories })
                    }
                    if (meals.isEmpty()) {
                        item {
                            Text(
                                "Aucun aliment enregistré",
                                style = MaterialTheme.typography.bodySmall,
                                color = TelomerGray500,
                                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
                            )
                        }
                    }
                    for (meal in meals) {
                        items(meal.items, key = { it.id }) { item ->
                            SwipeToDismissBox(
                                state = rememberSwipeToDismissBoxState(
                                    confirmValueChange = { value ->
                                        if (value == SwipeToDismissBoxValue.EndToStart) {
                                            viewModel.deleteMealItem(item.id)
                                            true
                                        } else false
                                    }
                                ),
                                backgroundContent = {
                                    Box(
                                        Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(TelomerRed)
                                            .padding(horizontal = 20.dp),
                                        contentAlignment = Alignment.CenterEnd,
                                    ) {
                                        Icon(Icons.Default.Delete, null, tint = Color.White)
                                    }
                                },
                                enableDismissFromStartToEnd = false,
                            ) {
                                MealItemRow(item)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DateSelector(label: String, onPrevious: () -> Unit, onNext: () -> Unit, canGoNext: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Default.ChevronLeft, "Jour précédent")
        }
        Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        IconButton(onClick = onNext, enabled = canGoNext) {
            Icon(Icons.Default.ChevronRight, "Jour suivant")
        }
    }
}

@Composable
private fun CalorieSummaryCard(summary: DailySummary) {
    val goal = summary.goal
    val calTarget = goal?.caloriesKcal?.toFloat() ?: 2000f
    val calProgress = (summary.totalCalories / calTarget).coerceIn(0.0, 1.5).toFloat()
    val animatedProgress by animateFloatAsState(calProgress, label = "cal")

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Calorie circle
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(90.dp)) {
                    CircularProgressIndicator(
                        progress = { animatedProgress.coerceAtMost(1f) },
                        modifier = Modifier.fillMaxSize(),
                        color = if (calProgress > 1f) TelomerRed else TelomerBlue,
                        trackColor = TelomerGray100,
                        strokeWidth = 8.dp,
                        strokeCap = StrokeCap.Round,
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${summary.totalCalories.toInt()}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                        )
                        Text("/ ${calTarget.toInt()}", fontSize = 11.sp, color = TelomerGray500)
                        Text("kcal", fontSize = 10.sp, color = TelomerGray500)
                    }
                }
                Spacer(Modifier.width(20.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    MacroBar("Protéines", summary.totalProteins, goal?.proteinsG?.toDouble() ?: 60.0, TelomerGreen)
                    MacroBar("Glucides", summary.totalCarbs, goal?.carbsG?.toDouble() ?: 250.0, TelomerOrange)
                    MacroBar("Lipides", summary.totalFats, goal?.fatsG?.toDouble() ?: 70.0, TelomerRed)
                }
            }
        }
    }
}

@Composable
private fun MacroBar(label: String, current: Double, target: Double, color: Color) {
    val progress by animateFloatAsState(
        (current / target).coerceIn(0.0, 1.0).toFloat(),
        label = label,
    )
    Column {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, fontSize = 12.sp, color = TelomerGray500)
            Text("${current.toInt()}/${target.toInt()}g", fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(2.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.15f),
            strokeCap = StrokeCap.Round,
        )
    }
}

@Composable
private fun MealTypeHeader(type: MealType, totalCal: Double) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(type.icon, fontSize = 20.sp)
            Spacer(Modifier.width(8.dp))
            Text(type.labelFr, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
        if (totalCal > 0) {
            Text("${totalCal.toInt()} kcal", fontSize = 14.sp, color = TelomerGray500)
        }
    }
    HorizontalDivider(modifier = Modifier.padding(top = 4.dp), color = TelomerGray100)
}

@Composable
private fun MealItemRow(item: MealLogItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.foodItem.displayName,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${item.quantityG.toInt()}g",
                    fontSize = 12.sp,
                    color = TelomerGray500,
                )
            }
            Text(
                "${item.caloriesKcal?.toInt() ?: "—"} kcal",
                fontWeight = FontWeight.SemiBold,
                color = TelomerBlue,
            )
        }
    }
}
