package health.telomer.android.feature.nutrition.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import health.telomer.android.feature.nutrition.domain.model.FoodItem
import health.telomer.android.feature.nutrition.domain.model.MealType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodSearchScreen(
    navController: NavController,
    viewModel: FoodSearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    // Navigate back on success
    LaunchedEffect(state.addedSuccessfully) {
        if (state.addedSuccessfully) navController.popBackStack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rechercher un aliment") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Retour")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            // Search bar
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::updateQuery,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                placeholder = { Text("Ex: poulet, riz, banane…") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateQuery("") }) {
                            Icon(Icons.Default.Clear, "Effacer")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
            )

            if (state.isLoading && state.selectedFood == null) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TelomerBlue, modifier = Modifier.size(32.dp))
                }
            }

            state.error?.let {
                Text(it, color = TelomerRed, modifier = Modifier.padding(8.dp), fontSize = 13.sp)
            }

            // Selected food detail or results list
            if (state.selectedFood != null) {
                SelectedFoodDetail(
                    food = state.selectedFood!!,
                    quantityG = state.quantityG,
                    mealType = state.selectedMealType,
                    onQuantityChanged = viewModel::updateQuantity,
                    onMealTypeChanged = viewModel::updateMealType,
                    onAdd = viewModel::addToMeal,
                    onClear = viewModel::clearSelection,
                    isLoading = state.isLoading,
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    items(state.results, key = { it.id }) { food ->
                        FoodResultCard(food = food, onClick = { viewModel.selectFood(food) })
                    }

                    if (state.results.isEmpty() && state.query.length >= 2 && !state.isLoading) {
                        item {
                            Text(
                                "Aucun résultat pour \"${state.query}\"",
                                modifier = Modifier.padding(16.dp),
                                color = TelomerGray500,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FoodResultCard(food: FoodItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        shape = RoundedCornerShape(10.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(food.displayName, fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "P:${food.proteinsG?.toInt() ?: "—"}g",
                        fontSize = 12.sp, color = TelomerGreen,
                    )
                    Text(
                        "G:${food.carbsG?.toInt() ?: "—"}g",
                        fontSize = 12.sp, color = TelomerOrange,
                    )
                    Text(
                        "L:${food.fatsG?.toInt() ?: "—"}g",
                        fontSize = 12.sp, color = TelomerRed,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${food.caloriesKcal?.toInt() ?: "—"}",
                    fontWeight = FontWeight.Bold, color = TelomerBlue,
                )
                Text("kcal/100g", fontSize = 10.sp, color = TelomerGray500)
            }
        }
    }
}

@Composable
private fun SelectedFoodDetail(
    food: FoodItem,
    quantityG: Double,
    mealType: MealType,
    onQuantityChanged: (Double) -> Unit,
    onMealTypeChanged: (MealType) -> Unit,
    onAdd: () -> Unit,
    onClear: () -> Unit,
    isLoading: Boolean,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = TelomerBlue.copy(alpha = 0.05f)),
            shape = RoundedCornerShape(12.dp),
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(food.displayName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    IconButton(onClick = onClear, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, "Fermer", modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    MacroInfo("Calories", "${food.caloriesKcal?.toInt() ?: "—"}", "kcal", TelomerBlue)
                    MacroInfo("Protéines", "${food.proteinsG?.toInt() ?: "—"}", "g", TelomerGreen)
                    MacroInfo("Glucides", "${food.carbsG?.toInt() ?: "—"}", "g", TelomerOrange)
                    MacroInfo("Lipides", "${food.fatsG?.toInt() ?: "—"}", "g", TelomerRed)
                }
                Text(
                    "pour 100g",
                    fontSize = 11.sp, color = TelomerGray500,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp),
                )
            }
        }

        // Quantity
        OutlinedTextField(
            value = quantityG.toInt().toString(),
            onValueChange = { it.toDoubleOrNull()?.let(onQuantityChanged) },
            label = { Text("Quantité (g)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        // Meal type selector
        Text("Type de repas", fontWeight = FontWeight.Medium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MealType.entries.forEach { type ->
                FilterChip(
                    selected = type == mealType,
                    onClick = { onMealTypeChanged(type) },
                    label = { Text("${type.icon} ${type.labelFr}", fontSize = 12.sp) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onAdd,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TelomerGreen),
            enabled = !isLoading,
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
            } else {
                Icon(Icons.Default.Add, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Ajouter au repas", fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun MacroInfo(label: String, value: String, unit: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = color)
        Text(unit, fontSize = 11.sp, color = TelomerGray500)
        Text(label, fontSize = 10.sp, color = TelomerGray500)
    }
}
