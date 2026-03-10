package health.telomer.android.auth

import android.net.Uri
import health.telomer.android.BuildConfig
import net.openid.appauth.AuthorizationServiceConfiguration

object AuthConfig {
    private val BASE_URL = "${BuildConfig.KEYCLOAK_URL}/realms/${BuildConfig.KEYCLOAK_REALM}"

    val ISSUER: Uri = Uri.parse(BASE_URL)
    val CLIENT_ID = BuildConfig.KEYCLOAK_CLIENT_ID
    val REDIRECT_URI: Uri = Uri.parse("health.telomer.android://callback")
    val END_SESSION_REDIRECT_URI: Uri = Uri.parse("health.telomer.android://callback")

    val DISCOVERY_URI: Uri = Uri.parse("$BASE_URL/.well-known/openid-configuration")

    val SCOPES = listOf("openid", "profile", "email", "offline_access")

    // Fallback endpoints if discovery fails
    val AUTH_ENDPOINT: Uri = Uri.parse("$BASE_URL/protocol/openid-connect/auth")
    val TOKEN_ENDPOINT: Uri = Uri.parse("$BASE_URL/protocol/openid-connect/token")
    val END_SESSION_ENDPOINT: Uri = Uri.parse("$BASE_URL/protocol/openid-connect/logout")
    val USERINFO_ENDPOINT: Uri = Uri.parse("$BASE_URL/protocol/openid-connect/userinfo")

    fun fallbackServiceConfig(): AuthorizationServiceConfiguration {
        return AuthorizationServiceConfiguration(AUTH_ENDPOINT, TOKEN_ENDPOINT)
    }
}
