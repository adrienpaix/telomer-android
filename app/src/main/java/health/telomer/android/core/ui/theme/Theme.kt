package health.telomer.android.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = TelomerBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEFF6FF),
    secondary = TelomerNavy,
    onSecondary = Color.White,
    background = TelomerBackground,
    surface = TelomerSurface,
    onBackground = TelomerOnSurface,
    onSurface = TelomerOnSurface,
    outline = TelomerOutline,
    error = TelomerRed,
)

private val DarkColorScheme = darkColorScheme(
    primary = TelomerBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E3A5F),
    secondary = Color(0xFF94A3B8),
    onSecondary = Color.White,
    background = TelomerDarkBackground,
    surface = TelomerDarkSurface,
    onBackground = TelomerDarkOnSurface,
    onSurface = TelomerDarkOnSurface,
    outline = TelomerDarkOutline,
    error = TelomerRed,
)

@Composable
fun TelomerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content,
    )
}
