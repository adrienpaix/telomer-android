package health.telomer.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

class HealthConnectPermissionsRationale : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            "🏥",
                            style = MaterialTheme.typography.displayLarge,
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Telomer Health utilise Health Connect",
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Nous accédons à vos données de santé (pas, fréquence cardiaque, sommeil, poids) pour les synchroniser avec votre dossier médical et permettre un suivi personnalisé par votre praticien.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(24.dp))
                        Button(onClick = { finish() }) {
                            Text("Compris")
                        }
                    }
                }
            }
        }
    }
}
