package health.telomer.android.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import health.telomer.android.feature.appointments.AppointmentBookingScreen
import health.telomer.android.feature.appointments.AppointmentsScreen
import health.telomer.android.feature.dashboard.DashboardScreen
import health.telomer.android.feature.documents.DocumentsScreen
import health.telomer.android.feature.messaging.ConversationScreen
import health.telomer.android.feature.messaging.MessagingScreen
import health.telomer.android.feature.nutrition.ui.journal.NutritionJournalScreen
import health.telomer.android.feature.nutrition.ui.camera.FoodCameraScreen
import health.telomer.android.feature.nutrition.ui.scanner.BarcodeScannerScreen
import health.telomer.android.feature.nutrition.ui.search.FoodSearchScreen
import health.telomer.android.feature.nutrition.ui.goals.NutritionGoalsScreen
import health.telomer.android.feature.practitioners.PractitionersScreen
import health.telomer.android.feature.prescriptions.PrescriptionsScreen
import health.telomer.android.feature.profile.ProfileScreen

sealed class BottomTab(val route: String, val label: String, val icon: ImageVector) {
    data object Dashboard : BottomTab("dashboard", "Accueil", Icons.Default.Home)
    data object Appointments : BottomTab("appointments", "RDV", Icons.Default.CalendarMonth)
    data object Nutrition : BottomTab("nutrition", "Nutrition", Icons.Default.Restaurant)
    data object Messages : BottomTab("messages", "Messages", Icons.Default.Email)
    data object Profile : BottomTab("profile", "Profil", Icons.Default.Person)
}

val tabs = listOf(
    BottomTab.Dashboard,
    BottomTab.Appointments,
    BottomTab.Nutrition,
    BottomTab.Messages,
    BottomTab.Profile,
)

// Routes that should show the bottom bar
private val bottomBarRoutes = tabs.map { it.route }.toSet()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelomerNavHost() {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStackEntry?.destination
    val showBottomBar = currentDestination?.route in bottomBarRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = BottomTab.Dashboard.route,
            modifier = Modifier.padding(padding),
        ) {
            // Bottom tabs
            composable(BottomTab.Dashboard.route) { DashboardScreen(navController) }
            composable(BottomTab.Appointments.route) { AppointmentsScreen(navController) }
            composable(BottomTab.Nutrition.route) { NutritionJournalScreen(navController) }
            composable(BottomTab.Messages.route) { MessagingScreen(navController) }
            composable(BottomTab.Profile.route) { ProfileScreen(navController) }

            // Existing sub-screens
            composable("appointment_booking") { AppointmentBookingScreen(navController) }
            composable("documents") { DocumentsScreen(navController) }
            composable("prescriptions") { PrescriptionsScreen(navController) }
            composable("practitioners") { PractitionersScreen(navController) }
            composable(
                route = "conversation/{conversationId}",
                arguments = listOf(navArgument("conversationId") { type = NavType.StringType }),
            ) {
                ConversationScreen(navController)
            }

            // Nutrition sub-screens
            composable("nutrition/camera") { FoodCameraScreen(navController) }
            composable("nutrition/scanner") { BarcodeScannerScreen(navController) }
            composable("nutrition/search") { FoodSearchScreen(navController) }
            composable("nutrition/goals") { NutritionGoalsScreen(navController) }
        }
    }
}
