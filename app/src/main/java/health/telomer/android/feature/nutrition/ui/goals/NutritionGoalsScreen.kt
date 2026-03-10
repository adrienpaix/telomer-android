package health.telomer.android.feature.nutrition.ui.goals

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import health.telomer.android.core.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionGoalsScreen(
    navController: NavController,
    viewModel: NutritionGoalsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Objectifs nutritionnels") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleEdit() }) {
                        Icon(
                            if (state.isEditing) Icons.Default.Close else Icons.Default.Edit,
                            if (state.isEditing) "Annuler" else "Modifier",
                        )
                    }
                },
            )
        },
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TelomerBlue)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            state.error?.let {
                Card(colors = CardDefaults.cardColors(containerColor = TelomerRed.copy(alpha = 0.1f))) {
                    Text(it, modifier = Modifier.padding(12.dp), color = TelomerRed)
                }
            }

            if (state.saved) {
                Card(colors = CardDefaults.cardColors(containerColor = TelomerGreen.copy(alpha = 0.1f))) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = TelomerGreen, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Objectifs sauvegardés !", color = TelomerGreen, fontWeight = FontWeight.Medium)
                    }
                }
            }

            GoalCard(
                title = "🔥 Calories",
                value = state.calories,
                unit = "kcal/jour",
                color = TelomerBlue,
                range = 1000f..4000f,
                isEditing = state.isEditing,
                onValueChanged = viewModel::updateCalories,
            )

            GoalCard(
                title = "💪 Protéines",
                value = state.proteins,
                unit = "g/jour",
                color = TelomerGreen,
                range = 20f..300f,
                isEditing = state.isEditing,
                onValueChanged = viewModel::updateProteins,
            )

            GoalCard(
                title = "🌾 Glucides",
                value = state.carbs,
                unit = "g/jour",
                color = TelomerOrange,
                range = 50f..500f,
                isEditing = state.isEditing,
                onValueChanged = viewModel::updateCarbs,
            )

            GoalCard(
                title = "🫒 Lipides",
                value = state.fats,
                unit = "g/jour",
                color = TelomerRed,
                range = 20f..200f,
                isEditing = state.isEditing,
                onValueChanged = viewModel::updateFats,
            )

            if (state.isEditing) {
                Button(
                    onClick = viewModel::save,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TelomerBlue),
                ) {
                    Icon(Icons.Default.Save, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Sauvegarder", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun GoalCard(
    title: String,
    value: Int,
    unit: String,
    color: Color,
    range: ClosedFloatingPointRange<Float>,
    isEditing: Boolean,
    onValueChanged: (Int) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text(
                    "$value $unit",
                    fontWeight = FontWeight.Bold,
                    color = color,
                    fontSize = 18.sp,
                )
            }

            if (isEditing) {
                Spacer(Modifier.height(8.dp))
                Slider(
                    value = value.toFloat(),
                    onValueChange = { onValueChanged(it.toInt()) },
                    valueRange = range,
                    colors = SliderDefaults.colors(
                        thumbColor = color,
                        activeTrackColor = color,
                        inactiveTrackColor = color.copy(alpha = 0.2f),
                    ),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${range.start.toInt()}", fontSize = 11.sp, color = TelomerGray500)
                    Text("${range.endInclusive.toInt()}", fontSize = 11.sp, color = TelomerGray500)
                }
            }
        }
    }
}
