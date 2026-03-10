package health.telomer.android.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.openid.appauth.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@Singleton
class AuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tokenManager: TokenManager,
    private val authService: AuthorizationService,
) {
    companion object {
        private const val TAG = "AuthManager"
        const val RC_AUTH = 1001
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    val accessToken: String? get() = tokenManager.accessToken

    private var serviceConfig: AuthorizationServiceConfiguration? = null

    init {
        // Initialize login state
        scope.launch {
            tokenManager.isLoggedInFlow.collect { loggedIn ->
                _isLoggedIn.value = loggedIn
            }
        }
    }

    suspend fun discoverConfiguration(): AuthorizationServiceConfiguration {
        serviceConfig?.let { return it }
        return suspendCoroutine { cont ->
            AuthorizationServiceConfiguration.fetchFromIssuer(AuthConfig.ISSUER) { config, ex ->
                if (config != null) {
                    serviceConfig = config
                    cont.resume(config)
                } else {
                    Log.w(TAG, "Discovery failed, using fallback", ex)
                    val fallback = AuthConfig.fallbackServiceConfig()
                    serviceConfig = fallback
                    cont.resume(fallback)
                }
            }
        }
    }

    fun login(activity: Activity) {
        scope.launch {
            try {
                val config = discoverConfiguration()
                val authRequest = AuthorizationRequest.Builder(
                    config,
                    AuthConfig.CLIENT_ID,
                    ResponseTypeValues.CODE,
                    AuthConfig.REDIRECT_URI,
                ).setScopes(AuthConfig.SCOPES)
                    .setCodeVerifier(CodeVerifierUtil.generateRandomCodeVerifier())
                    .build()

                val authIntent = authService.getAuthorizationRequestIntent(authRequest)
                activity.startActivityForResult(authIntent, RC_AUTH)
            } catch (e: Exception) {
                Log.e(TAG, "Login failed", e)
            }
        }
    }

    suspend fun handleAuthResponse(intent: Intent): Boolean {
        val response = AuthorizationResponse.fromIntent(intent)
        val exception = AuthorizationException.fromIntent(intent)

        if (response == null) {
            Log.e(TAG, "Auth response null", exception)
            return false
        }

        return try {
            val tokenResponse = exchangeCode(response)
            saveTokenResponse(tokenResponse)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Token exchange failed", e)
            false
        }
    }

    private suspend fun exchangeCode(response: AuthorizationResponse): TokenResponse {
        return suspendCoroutine { cont ->
            authService.performTokenRequest(
                response.createTokenExchangeRequest()
            ) { tokenResponse, ex ->
                if (tokenResponse != null) {
                    cont.resume(tokenResponse)
                } else {
                    cont.resumeWithException(
                        ex ?: Exception("Token exchange failed")
                    )
                }
            }
        }
    }

    suspend fun refreshToken(): Boolean {
        val refreshToken = tokenManager.refreshToken ?: return false
        val config = try { discoverConfiguration() } catch (_: Exception) { return false }

        return try {
            val request = TokenRequest.Builder(config, AuthConfig.CLIENT_ID)
                .setGrantType(GrantTypeValues.REFRESH_TOKEN)
                .setRefreshToken(refreshToken)
                .build()

            val tokenResponse = suspendCoroutine<TokenResponse> { cont ->
                authService.performTokenRequest(request) { resp, ex ->
                    if (resp != null) {
                        cont.resume(resp)
                    } else {
                        cont.resumeWithException(
                            ex ?: Exception("Refresh failed")
                        )
                    }
                }
            }
            saveTokenResponse(tokenResponse)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Token refresh failed", e)
            false
        }
    }

    /**
     * Ensures a valid access token is available, refreshing if necessary.
     * Returns the token or null if both access and refresh are invalid.
     */
    suspend fun getValidAccessToken(): String? {
        val token = tokenManager.accessToken
        if (token != null && !tokenManager.isExpired()) {
            return token
        }
        // Token expired, try refresh
        return if (refreshToken()) {
            tokenManager.accessToken
        } else {
            // Refresh failed, user needs to re-login
            tokenManager.clear()
            null
        }
    }

    suspend fun logout() {
        val idToken = tokenManager.idToken
        val config = try { discoverConfiguration() } catch (_: Exception) { null }

        // Clear local tokens first
        tokenManager.clear()

        // Try to end session on Keycloak
        if (idToken != null && config != null) {
            try {
                val endSessionEndpoint = config.discoveryDoc
                    ?.docJson
                    ?.optString("end_session_endpoint")
                    ?.let { Uri.parse(it) }
                    ?: AuthConfig.END_SESSION_ENDPOINT

                val endSessionRequest = EndSessionRequest.Builder(config)
                    .setIdTokenHint(idToken)
                    .setPostLogoutRedirectUri(AuthConfig.END_SESSION_REDIRECT_URI)
                    .build()

                // Fire and forget — the local state is already cleared
                Log.d(TAG, "End session request sent to $endSessionEndpoint")
            } catch (e: Exception) {
                Log.w(TAG, "End session failed (tokens already cleared)", e)
            }
        }
    }

    private suspend fun saveTokenResponse(tokenResponse: TokenResponse) {
        val expiresAt = tokenResponse.accessTokenExpirationTime
            ?: (System.currentTimeMillis() + 300_000) // default 5 min

        tokenManager.saveTokens(
            accessToken = tokenResponse.accessToken!!,
            refreshToken = tokenResponse.refreshToken,
            idToken = tokenResponse.idToken,
            expiresAtMillis = expiresAt,
        )
    }
}
