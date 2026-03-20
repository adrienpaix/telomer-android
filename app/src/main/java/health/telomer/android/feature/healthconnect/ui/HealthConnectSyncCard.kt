package health.telomer.android.feature.healthconnect.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import health.telomer.android.core.ui.theme.TelomerBlue
import health.telomer.android.feature.healthconnect.data.SyncResult
import java.time.Instant

@Composable
internal fun SyncButton(
    isSyncing: Boolean,
    lastSyncEpoch: Long?,
    syncResult: SyncResult?,
    backendSyncCount: Int?,
    backendSyncError: String?,
    onSync: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val rotation = if (isSyncing) {
        val infiniteTransition = rememberInfiniteTransition(label = "sync")
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(animation = tween(1000, easing = LinearEasing)),
            label = "syncRotation",
        ).value
    } else 0f

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onSync()
            },
            enabled = !isSyncing,
            colors = ButtonDefaults.buttonColors(
                containerColor = TelomerBlue,
                disabledContainerColor = TelomerBlue.copy(alpha = 0.5f),
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(20.dp).rotate(rotation))
            Spacer(Modifier.width(8.dp))
            Text(if (isSyncing) "Synchronisation…" else "Synchroniser", fontSize = 16.sp)
        }
        Spacer(Modifier.height(8.dp))
        lastSyncEpoch?.let { epoch ->
            Text(
                "Dernière sync : ${formatRelativeTime(epoch)}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        }
        syncResult?.let { r ->
            Spacer(Modifier.height(4.dp))
            Text(
                "${r.synced} envoyé(s) · ${r.duplicates} doublon(s)",
                style = MaterialTheme.typography.bodySmall,
                color = ActivityGreen,
            )
        }
        backendSyncCount?.let { count ->
            Spacer(Modifier.height(4.dp))
            Text(
                "Synchronisé ✓ ($count métriques envoyées)",
                style = MaterialTheme.typography.bodySmall,
                color = ActivityGreen,
            )
        }
        backendSyncError?.let { err ->
            Spacer(Modifier.height(4.dp))
            Text(err, style = MaterialTheme.typography.bodySmall, color = CardioRed)
        }
    }
}

internal fun formatRelativeTime(epochSec: Long): String {
    val now = Instant.now().epochSecond
    val diff = now - epochSec
    return when {
        diff < 60 -> "à l'instant"
        diff < 3600 -> "il y a ${diff / 60} min"
        diff < 86400 -> "il y a ${diff / 3600}h"
        else -> "il y a ${diff / 86400}j"
    }
}
