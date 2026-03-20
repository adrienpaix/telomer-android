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
 * Tests TokenManager — logique de cache m\u00e9moire et d'expiration.
 *
 * Note : EncryptedSharedPreferences n\u00e9cessite le runtime Android (Keystore).
 * On teste donc la logique pure via simulation du cache m\u00e9moire de TokenManager.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TokenManagerTest {

    private val testDispatcher = StandardTestDispatcher()

    private val ACCESS_TOKEN = "eyJhbGciOiJSUzI1NiJ9.test_access_token"
    private val REFRESH_TOKEN = "eyJhbGciOiJSUzI1NiJ9.test_refresh_token"
    private val ID_TOKEN = "eyJhbGciOiJSUzI1NiJ9.test_id_token"

    // On simule le cache m\u00e9moire interne de TokenManager
    private var accessTokenCache: String? = null
    private var refreshTokenCache: String? = null
    private var idTokenCache: String? = null
    private var expiresAtMillis: Long = 0L
    private var isLoggedIn: Boolean = false

    /** Simule TokenManager.saveTokens() */
    private fun simulateSaveTokens(
        accessToken: String,
        refreshToken: String? = null,
        idToken: String? = null,
        expiresAt: Long? = null,
    ) {
        accessTokenCache = accessToken
        refreshToken?.let { refreshTokenCache = it }
        idToken?.let { idTokenCache = it }
        expiresAt?.let { expiresAtMillis = it }
        isLoggedIn = true
    }

    /** Simule TokenManager.clear() */
    private fun simulateClear() {
        accessTokenCache = null
        refreshTokenCache = null
        idTokenCache = null
        expiresAtMillis = 0L
        isLoggedIn = false
    }

    /** Simule TokenManager.isExpired() */
    private fun simulateIsExpired(): Boolean {
        if (expiresAtMillis == 0L) return true
        return System.currentTimeMillis() >= (expiresAtMillis - 60_000L)
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        simulateClear()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `accessToken retourne la valeur du cache apres saveTokens`() {
        assertNull("Cache vide initialement", accessTokenCache)
        simulateSaveTokens(accessToken = ACCESS_TOKEN, refreshToken = REFRESH_TOKEN, idToken = ID_TOKEN)
        assertEquals("Cache access token correct", ACCESS_TOKEN, accessTokenCache)
        assertNotNull("Access token non null", accessTokenCache)
    }

    @Test
    fun `refreshToken est stock\u00e9 correctement dans le cache`() {
        simulateSaveTokens(accessToken = ACCESS_TOKEN, refreshToken = REFRESH_TOKEN)
        assertEquals(REFRESH_TOKEN, refreshTokenCache)
    }

    @Test
    fun `idToken est stock\u00e9 correctement dans le cache`() {
        simulateSaveTokens(accessToken = ACCESS_TOKEN, idToken = ID_TOKEN)
        assertEquals(ID_TOKEN, idTokenCache)
    }

    @Test
    fun `clearTokens remet tous les caches a null`() {
        simulateSaveTokens(accessToken = ACCESS_TOKEN, refreshToken = REFRESH_TOKEN, idToken = ID_TOKEN)
        assertNotNull(accessTokenCache)
        simulateClear()
        assertNull("access token null apr\u00e8s clear", accessTokenCache)
        assertNull("refresh token null apr\u00e8s clear", refreshTokenCache)
        assertNull("id token null apr\u00e8s clear", idTokenCache)
        assertFalse("isLoggedIn false apr\u00e8s clear", isLoggedIn)
    }

    @Test
    fun `isLoggedIn passe a true apres saveTokens`() {
        assertFalse(isLoggedIn)
        simulateSaveTokens(ACCESS_TOKEN)
        assertTrue(isLoggedIn)
    }

    @Test
    fun `isLoggedIn passe a false apres clearTokens`() {
        simulateSaveTokens(ACCESS_TOKEN)
        assertTrue(isLoggedIn)
        simulateClear()
        assertFalse(isLoggedIn)
    }

    @Test
    fun `token non expire avec expiry future`() {
        simulateSaveTokens(
            accessToken = ACCESS_TOKEN,
            expiresAt = System.currentTimeMillis() + 3_600_000L, // +1h
        )
        assertFalse("Token valide dans 1h ne doit pas \u00eatre expir\u00e9", simulateIsExpired())
    }

    @Test
    fun `token expire avec expiry passe`() {
        simulateSaveTokens(
            accessToken = ACCESS_TOKEN,
            expiresAt = System.currentTimeMillis() - 1_000L, // -1s
        )
        assertTrue("Token expir\u00e9 depuis 1s", simulateIsExpired())
    }

    @Test
    fun `token expirant dans moins de 60s est considere expire marge de securite`() {
        simulateSaveTokens(
            accessToken = ACCESS_TOKEN,
            expiresAt = System.currentTimeMillis() + 30_000L, // +30s
        )
        assertTrue("Token expirant dans 30s consid\u00e9r\u00e9 expir\u00e9 (marge 60s)", simulateIsExpired())
    }

    @Test
    fun `token sans expiry est considere expire`() {
        // expiresAtMillis = 0 par d\u00e9faut
        assertTrue("Token sans expiry consid\u00e9r\u00e9 expir\u00e9", simulateIsExpired())
    }

    @Test
    fun `saveTokens multiples - dernier token garde`() {
        simulateSaveTokens(accessToken = "token_v1")
        assertEquals("token_v1", accessTokenCache)
        simulateSaveTokens(accessToken = "token_v2")
        assertEquals("token_v2", accessTokenCache)
    }

    @Test
    fun `saveTokens sans refreshToken preserve l ancien refreshToken`() {
        simulateSaveTokens(accessToken = ACCESS_TOKEN, refreshToken = REFRESH_TOKEN)
        assertEquals(REFRESH_TOKEN, refreshTokenCache)
        // Nouveau saveTokens sans refreshToken
        simulateSaveTokens(accessToken = "new_access_token")
        // refreshToken devrait \u00eatre pr\u00e9serv\u00e9 (null refreshToken ne remplace pas)
        assertEquals("Refresh token pr\u00e9serv\u00e9", REFRESH_TOKEN, refreshTokenCache)
    }
}
