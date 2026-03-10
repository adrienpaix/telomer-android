package health.telomer.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import health.telomer.android.auth.AuthManager
import health.telomer.android.auth.AuthViewModel
import health.telomer.android.core.ui.theme.TelomerTheme
import health.telomer.android.navigation.TelomerNavHost

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle auth callback if launched via redirect URI
        intent?.let { handleAuthIntent(it) }

        setContent {
            TelomerTheme {
                TelomerNavHost(authViewModel = authViewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleAuthIntent(intent)
    }

    @Deprecated("Use Activity Result API")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AuthManager.RC_AUTH && data != null) {
            authViewModel.handleCallback(data)
        }
    }

    private fun handleAuthIntent(intent: Intent) {
        val data = intent.data
        if (data != null && data.scheme == "health.telomer.android" && data.host == "callback") {
            authViewModel.handleCallback(intent)
        }
    }
}
