package health.telomer.android.core.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = TelomerBlue,
    onPrimary = Color.White,
    primaryContainer = TelomerBlue.copy(alpha = 0.1f),
    secondary = TelomerNavy,
    onSecondary = Color.White,
    background = TelomerBackground,
    surface = Color.White,
    onBackground = TelomerGray900,
    onSurface = TelomerGray900,
    error = TelomerRed,
)

@Composable
fun TelomerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography(),
        content = content,
    )
}
