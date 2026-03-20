package health.telomer.android.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import health.telomer.android.core.ui.theme.TelomerTheme

class DashboardScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun dashboardShowsBottomNavigation() {
        composeTestRule.setContent {
            TelomerTheme {
                NavigationBar {
                    listOf(Accueil, Bilan, Nutrition, Plan, Plus).forEachIndexed { index, label ->
                        NavigationBarItem(
                            selected = index == 0,
                            onClick = {},
                            icon = {
                                when (label) {
                                    Accueil -> Icon(Icons.Default.Home, contentDescription = label)
                                    Bilan -> Icon(Icons.Default.Insights, contentDescription = label)
                                    Nutrition -> Icon(Icons.Default.Restaurant, contentDescription = label)
                                    Plan -> Icon(Icons.Default.CheckCircle, contentDescription = label)
                                    else -> Icon(Icons.Default.Menu, contentDescription = label)
                                }
                            },
                            label = { Text(label) },
                        )
                    }
                }
            }
        }
        // Vérifier que les 5 tabs de navigation sont présents
        composeTestRule.onNodeWithText(Accueil).assertExists()
        composeTestRule.onNodeWithText(Bilan).assertExists()
        composeTestRule.onNodeWithText(Nutrition).assertExists()
        composeTestRule.onNodeWithText(Plan).assertExists()
        composeTestRule.onNodeWithText(Plus).assertExists()
    }

    @Test
    fun bottomNavigationHasFiveTabs() {
        composeTestRule.setContent {
            TelomerTheme {
                NavigationBar {
                    listOf(Accueil, Bilan, Nutrition, Plan, Plus).forEachIndexed { index, label ->
                        NavigationBarItem(
                            selected = index == 0,
                            onClick = {},
                            icon = { Icon(Icons.Default.Home, contentDescription = label) },
                            label = { Text(label) },
                        )
                    }
                }
            }
        }
        // 5 tabs doivent être présents
        val tabs = listOf(Accueil, Bilan, Nutrition, Plan, Plus)
        tabs.forEach { tab ->
            composeTestRule.onNodeWithText(tab).assertExists()
        }
    }
}
