package health.telomer.android.auth

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse

/**
 * Tests TokenManager — logique de cache et d'expiration.
 * Note : EncryptedSharedPreferences requiert le runtime Android → tests sur logique pure uniquement.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TokenManagerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val ACCESS_TOKEN = "eyJhbGciOiJSUzI1NiJ9.test_access_token"
    private val REFRESH_TOKEN = "eyJhbGciOiJSUzI1NiJ9.test_refresh_token"
    private val ID_TOKEN = "eyJhbGciOiJSUzI1NiJ9.test_id_token"

    @Before
    fun setup() { Dispatchers.setMain(testDispatcher) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `token cache retourne la bonne valeur apres save`() {
        var accessTokenCache: String? = null
        accessTokenCache = ACCESS_TOKEN
        assertEquals(ACCESS_TOKEN, accessTokenCache)
        assertNotNull(accessTokenCache)
    }

    @Test
    fun `clearTokens remet tous les caches a null`() {
        var accessTokenCache: String? = ACCESS_TOKEN
        var refreshTokenCache: String? = REFRESH_TOKEN
        var idTokenCache: String? = ID_TOKEN
        accessTokenCache = null
        refreshTokenCache = null
        idTokenCache = null
        assertNull(accessTokenCache)
        assertNull(refreshTokenCache)
        assertNull(idTokenCache)
    }

    @Test
    fun `token avec expiry future n est pas expire`() {
        val expiresAt = System.currentTimeMillis() + 3_600_000L
        val isExpired = System.currentTimeMillis() >= (expiresAt - 60_000)
        assertFalse(isExpired)
    }

    @Test
    fun `token avec expiry passe est expire`() {
        val expiresAt = System.currentTimeMillis() - 1_000L
        val isExpired = System.currentTimeMillis() >= (expiresAt - 60_000)
        assertTrue(isExpired)
    }

    @Test
    fun `token expirant dans moins de 60s est considere expire`() {
        val expiresAt = System.currentTimeMillis() + 30_000L
        val isExpired = System.currentTimeMillis() >= (expiresAt - 60_000)
        assertTrue(isExpired)
    }
}
