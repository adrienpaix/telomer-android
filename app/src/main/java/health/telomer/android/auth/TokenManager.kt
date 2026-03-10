package health.telomer.android.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "auth_prefs")

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private val ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val ID_TOKEN = stringPreferencesKey("id_token")
        private val EXPIRES_AT = longPreferencesKey("expires_at")
    }

    val accessToken: String?
        get() = runBlocking {
            context.dataStore.data.map { it[ACCESS_TOKEN] }.first()
        }

    val refreshToken: String?
        get() = runBlocking {
            context.dataStore.data.map { it[REFRESH_TOKEN] }.first()
        }

    val idToken: String?
        get() = runBlocking {
            context.dataStore.data.map { it[ID_TOKEN] }.first()
        }

    val isLoggedInFlow: Flow<Boolean>
        get() = context.dataStore.data.map { prefs ->
            !prefs[ACCESS_TOKEN].isNullOrBlank()
        }

    fun isExpired(): Boolean {
        val expiresAt = runBlocking {
            context.dataStore.data.map { it[EXPIRES_AT] }.first()
        } ?: return true
        // Consider expired 60 seconds before actual expiry for safety margin
        return System.currentTimeMillis() >= (expiresAt - 60_000)
    }

    suspend fun saveTokens(
        accessToken: String,
        refreshToken: String?,
        idToken: String? = null,
        expiresAtMillis: Long? = null,
    ) {
        context.dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN] = accessToken
            refreshToken?.let { prefs[REFRESH_TOKEN] = it }
            idToken?.let { prefs[ID_TOKEN] = it }
            expiresAtMillis?.let { prefs[EXPIRES_AT] = it }
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
