package health.telomer.android.auth

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthState {
    data object Loading : AuthState()
    data object LoggedOut : AuthState()
    data class LoggedIn(val displayName: String? = null) : AuthState()
    data class Error(val message: String) : AuthState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authManager: AuthManager,
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        viewModelScope.launch {
            authManager.isLoggedIn.collect { loggedIn ->
                _authState.value = if (loggedIn) {
                    AuthState.LoggedIn()
                } else {
                    AuthState.LoggedOut
                }
            }
        }
    }

    fun login(activity: android.app.Activity) {
        _authState.value = AuthState.Loading
        authManager.login(activity)
    }

    fun handleCallback(intent: Intent) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val success = authManager.handleAuthResponse(intent)
            if (!success) {
                _authState.value = AuthState.Error("Échec de l'authentification. Veuillez réessayer.")
            }
            // If success, the isLoggedIn flow will update the state automatically
        }
    }

    fun logout() {
        viewModelScope.launch {
            authManager.logout()
        }
    }

    fun clearError() {
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.LoggedOut
        }
    }
}
