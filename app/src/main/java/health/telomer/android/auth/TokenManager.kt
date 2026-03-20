package health.telomer.android.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val PREFS_NAME = "secure_auth_prefs"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_ID_TOKEN = "id_token"
        private const val KEY_EXPIRES_AT = "expires_at"
    }

    private val encryptedPrefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    // Cache mémoire pour éviter tout runBlocking sur le main thread
    private var _accessTokenCache: String? = null
    private var _refreshTokenCache: String? = null
    private var _idTokenCache: String? = null

    // StateFlow pour observer l'état de connexion (remplace l'ancien DataStore Flow)
    private val _isLoggedInFlow = MutableStateFlow(
        !encryptedPrefs.getString(KEY_ACCESS_TOKEN, null).isNullOrBlank()
    )
    val isLoggedInFlow: Flow<Boolean> = _isLoggedInFlow

    val accessToken: String?
        get() = _accessTokenCache ?: encryptedPrefs.getString(KEY_ACCESS_TOKEN, null)
            .also { _accessTokenCache = it }

    val refreshToken: String?
        get() = _refreshTokenCache ?: encryptedPrefs.getString(KEY_REFRESH_TOKEN, null)
            .also { _refreshTokenCache = it }

    val idToken: String?
        get() = _idTokenCache ?: encryptedPrefs.getString(KEY_ID_TOKEN, null)
            .also { _idTokenCache = it }

    fun isExpired(): Boolean {
        val expiresAt = encryptedPrefs.getLong(KEY_EXPIRES_AT, 0L)
        if (expiresAt == 0L) return true
        // Consider expired 60 seconds before actual expiry for safety margin
        return System.currentTimeMillis() >= (expiresAt - 60_000)
    }

    suspend fun saveTokens(
        accessToken: String,
        refreshToken: String?,
        idToken: String? = null,
        expiresAtMillis: Long? = null,
    ) {
        encryptedPrefs.edit().apply {
            putString(KEY_ACCESS_TOKEN, accessToken)
            refreshToken?.let { putString(KEY_REFRESH_TOKEN, it) }
            idToken?.let { putString(KEY_ID_TOKEN, it) }
            expiresAtMillis?.let { putLong(KEY_EXPIRES_AT, it) }
        }.apply()

        // Mettre à jour le cache mémoire
        _accessTokenCache = accessToken
        _refreshTokenCache = refreshToken ?: _refreshTokenCache
        _idTokenCache = idToken ?: _idTokenCache
        _isLoggedInFlow.value = true
    }

    suspend fun clear() {
        encryptedPrefs.edit().clear().apply()
        _accessTokenCache = null
        _refreshTokenCache = null
        _idTokenCache = null
        _isLoggedInFlow.value = false
    }
}
