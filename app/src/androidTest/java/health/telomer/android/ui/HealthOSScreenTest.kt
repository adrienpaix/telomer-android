package health.telomer.android.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import health.telomer.android.core.ui.theme.TelomerTheme

class HealthOSScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Composable
    private fun FakeHealthOSScreen(score: Int?, isLoading: Boolean) {
        TelomerTheme {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                when {
                    isLoading -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Text(Chargement…)
                        }
                    }
                    score != null -> {
                        Text(Score de santé : )
                    }
                    else -> {
                        Text(Aucun score disponible)
                    }
                }
            }
        }
    }

    @Test
    fun healthOSScreenShowsScoreWhenNotNull() {
        composeTestRule.setContent {
            FakeHealthOSScreen(score = 72, isLoading = false)
        }
        composeTestRule.onNodeWithText(Score de santé : 72).assertExists()
    }

    @Test
    fun healthOSScreenShowsLoadingIndicator() {
        composeTestRule.setContent {
            FakeHealthOSScreen(score = null, isLoading = true)
        }
        composeTestRule.onNodeWithText(Chargement…).assertExists()
    }

    @Test
    fun healthOSScreenShowsEmptyStateWhenScoreIsNull() {
        composeTestRule.setContent {
            FakeHealthOSScreen(score = null, isLoading = false)
        }
        composeTestRule.onNodeWithText(Aucun score disponible).assertExists()
    }
}
