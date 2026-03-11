package health.telomer.android.feature.actionplan

import androidx.compose.animation.animateContentSize
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import health.telomer.android.core.ui.theme.*

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
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TelomerBlue)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Progress header
                item {
                    ProgressHeader(
                        completed = state.completedCount,
                        total = state.totalCount,
                    )
                }

                // Sections
                items(state.sections) { section ->
                    SectionCard(
                        section = section,
                        onToggleItem = { itemIndex ->
                            viewModel.toggleItem(section.title, itemIndex)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressHeader(completed: Int, total: Int) {
    val progress = if (total > 0) completed.toFloat() / total else 0f

    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = TelomerBlue),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        "Progression",
                        color = TelomerWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    )
                    Text(
                        "$completed / $total objectifs complétés",
                        color = TelomerWhite.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                    )
                }
                Text(
                    "${(progress * 100).toInt()}%",
                    color = TelomerWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                )
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = TelomerWhite,
                trackColor = TelomerWhite.copy(alpha = 0.3f),
                drawStopIndicator = {},
            )
        }
    }
}

@Composable
private fun SectionCard(
    section: ActionPlanSection,
    onToggleItem: (Int) -> Unit,
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = TelomerWhite),
        modifier = Modifier.animateContentSize(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val sectionCompleted = section.items.count { it.isChecked }
            val sectionTotal = section.items.size

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    section.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TelomerGray900,
                )
                Text(
                    "$sectionCompleted/$sectionTotal",
                    color = if (sectionCompleted == sectionTotal) TelomerGreen else TelomerGray500,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
            }

            Spacer(Modifier.height(8.dp))

            section.items.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = item.isChecked,
                        onCheckedChange = { onToggleItem(index) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = TelomerGreen,
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
