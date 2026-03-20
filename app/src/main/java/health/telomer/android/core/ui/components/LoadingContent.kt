package health.telomer.android.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import health.telomer.android.core.ui.theme.TelomerBlue
import health.telomer.android.core.ui.theme.TelomerRed

/**
 * Composant partagé pour les états de chargement, d'erreur, et de contenu.
 * Remplace le pattern répété dans DashboardScreen, HealthOSScreen, etc.
 */
@Composable
fun LoadingContent(
    isLoading: Boolean,
    error: String? = null,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TelomerBlue)
            }
            error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = TelomerRed,
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(error, color = MaterialTheme.colorScheme.onSurface)
                    if (onRetry != null) {
                        Spacer(Modifier.height(12.dp))
                        TextButton(onClick = onRetry) {
                            Text("Réessayer")
                        }
                    }
                }
            }
            else -> content()
        }
    }
}
