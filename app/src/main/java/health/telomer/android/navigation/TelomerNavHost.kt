package health.telomer.android.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import health.telomer.android.auth.AuthViewModel
import health.telomer.android.auth.AuthState
import health.telomer.android.auth.LoginScreen
import health.telomer.android.feature.actionplan.ActionPlanScreen
import health.telomer.android.feature.appointments.AppointmentBookingScreen
import health.telomer.android.feature.appointments.AppointmentsScreen
import health.telomer.android.feature.consultation.VideoCallScreen
import health.telomer.android.feature.dashboard.DashboardScreen
import health.telomer.android.feature.healthos.HealthOSScreen
import health.telomer.android.feature.documents.DocumentsScreen
import health.telomer.android.feature.healthconnect.ui.HealthConnectScreen
import health.telomer.android.feature.messaging.ConversationScreen
import health.telomer.android.feature.messaging.MessagingScreen
import health.telomer.android.feature.nutrition.ui.camera.FoodCameraScreen
import health.telomer.android.feature.nutrition.ui.goals.NutritionGoalsScreen
import health.telomer.android.feature.nutrition.ui.journal.NutritionJournalScreen
import health.telomer.android.feature.nutrition.ui.scanner.BarcodeScannerScreen
import health.telomer.android.feature.nutrition.ui.search.FoodSearchScreen
import health.telomer.android.feature.practitioners.PractitionersScreen
import health.telomer.android.feature.prescriptions.PrescriptionsScreen
import health.telomer.android.feature.profile.ProfileScreen

sealed class BottomTab(val route: String, val label: String, val icon: ImageVector) {
    data object Dashboard : BottomTab("dashboard", "Accueil", Icons.Default.Home)
    data object HealthOS : BottomTab("healthos", "Bilan", Icons.Default.Insights)
    data object Nutrition : BottomTab("nutrition", "Nutrition", Icons.Default.Restaurant)
    data object ActionPlan : BottomTab("action_plan", "Plan", Icons.Default.CheckCircle)
    data object More : BottomTab("more", "Plus", Icons.Default.Menu)
}

private val tabs = listOf(
    BottomTab.Dashboard,
    BottomTab.HealthOS,
    BottomTab.Nutrition,
    BottomTab.ActionPlan,
    BottomTab.More,
)

// Routes where bottom bar should be hidden
private val hideBottomBarRoutes = setOf(
    "appointment_booking", "documents", "prescriptions", "practitioners",
    "nutrition/camera", "nutrition/scanner", "nutrition/search", "nutrition/goals",
    "healthconnect",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreBottomSheet(navController: NavController, onDismiss: () -> Unit) {
    val items = listOf(
        Triple("messages", Icons.Default.Email, "Messages"),
        Triple("appointments", Icons.Default.CalendarMonth, "Rendez-vous"),
        Triple("healthconnect", Icons.Default.Watch, "Appareils connectés"),
        Triple("documents", Icons.Default.Folder, "Documents"),
        Triple("prescriptions", Icons.Default.LocalPharmacy, "Ordonnances"),
        Triple("profile", Icons.Default.Person, "Profil"),
    )
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Navigation",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            items.forEach { (route, icon, label) ->
                ListItem(
                    headlineContent = { Text(label) },
                    leadingContent = { Icon(icon, contentDescription = label) },
                    modifier = Modifier.clickable {
                        navController.navigate(route)
                        onDismiss()
                    },
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun TelomerNavHost(authViewModel: AuthViewModel = hiltViewModel()) {
    val authState by authViewModel.authState.collectAsState()

    when (authState) {
        is AuthState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is AuthState.LoggedOut, is AuthState.Error -> {
            LoginScreen(
                authState = authState,
                onLogin = { activity -> authViewModel.login(activity) },
                onClearError = { authViewModel.clearError() },
            )
        }
        is AuthState.LoggedIn -> {
            MainNavigation()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainNavigation() {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStackEntry?.destination
    val currentRoute = currentDestination?.route ?: ""
    var showMoreSheet by remember { mutableStateOf(false) }

    val showBottomBar = currentRoute !in hideBottomBarRoutes &&
        !currentRoute.startsWith("conversation/") &&
        !currentRoute.startsWith("consultation/")

    if (showMoreSheet) {
        MoreBottomSheet(
            navController = navController,
            onDismiss = { showMoreSheet = false },
        )
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = if (tab is BottomTab.More) false
                                       else currentDestination?.hierarchy?.any { it.route == tab.route } == true,
                            onClick = {
                                if (tab is BottomTab.More) {
                                    showMoreSheet = true
                                } else {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
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
            composable(
                route = BottomTab.Dashboard.route,
                deepLinks = listOf(navDeepLink { uriPattern = "https://app.telomer.health/dashboard" }),
            ) { DashboardScreen(navController) }
            composable(
                route = BottomTab.HealthOS.route,
                deepLinks = listOf(navDeepLink { uriPattern = "https://app.telomer.health/bilan" }),
            ) { HealthOSScreen(navController) }
            composable(BottomTab.Nutrition.route) { NutritionJournalScreen(navController) }
            composable(BottomTab.ActionPlan.route) { ActionPlanScreen(navController) }

            // Secondary screens — accessibles via MoreBottomSheet
            composable("appointments") { AppointmentsScreen(navController) }
            composable("messages") { MessagingScreen(navController) }
            composable("profile") { ProfileScreen(navController) }
            composable("healthconnect") { HealthConnectScreen(navController) }
            composable("documents") { DocumentsScreen(navController) }
            composable("prescriptions") { PrescriptionsScreen(navController) }
            composable("practitioners") { PractitionersScreen(navController) }

            // Sub-screens
            composable("appointment_booking") { AppointmentBookingScreen(navController) }

            // Conversation with argument
            composable(
                "conversation/{conversationId}",
                arguments = listOf(navArgument("conversationId") { type = NavType.StringType }),
            ) {
                ConversationScreen(navController = navController)
            }

            // Consultation (video call)
            composable(
                "consultation/{appointmentId}",
                arguments = listOf(navArgument("appointmentId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val appointmentId = backStackEntry.arguments?.getString("appointmentId") ?: return@composable
                VideoCallScreen(navController = navController, appointmentId = appointmentId)
            }

            // Nutrition sub-screens
            composable("nutrition/camera") { FoodCameraScreen(navController) }
            composable("nutrition/scanner") { BarcodeScannerScreen(navController) }
            composable("nutrition/search") { FoodSearchScreen(navController) }
            composable("nutrition/goals") { NutritionGoalsScreen(navController) }
        }
    }
}
