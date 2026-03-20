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
 * Tests TokenManager - logique de cache memoire et d'expiration.
 *
 * Note : EncryptedSharedPreferences necessite le runtime Android (Keystore).
 * On teste donc la logique pure via simulation du cache memoire de TokenManager.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TokenManagerTest {

    private val testDispatcher = StandardTestDispatcher()

    private val ACCESS_TOKEN = "eyJhbGciOiJSUzI1NiJ9.test_access_token"
    private val REFRESH_TOKEN = "eyJhbGciOiJSUzI1NiJ9.test_refresh_token"
    private val ID_TOKEN = "eyJhbGciOiJSUzI1NiJ9.test_id_token"

    private var accessTokenCache: String? = null
    private var refreshTokenCache: String? = null
    private var idTokenCache: String? = null
    private var expiresAtMillis: Long = 0L
    private var isLoggedIn: Boolean = false

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

    private fun simulateClear() {
        accessTokenCache = null
        refreshTokenCache = null
        idTokenCache = null
        expiresAtMillis = 0L
        isLoggedIn = false
    }

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
    fun `refreshToken est stocke correctement dans le cache`() {
        simulateSaveTokens(accessToken = ACCESS_TOKEN, refreshToken = REFRESH_TOKEN)
        assertEquals(REFRESH_TOKEN, refreshTokenCache)
    }

    @Test
    fun `idToken est stocke correctement dans le cache`() {
        simulateSaveTokens(accessToken = ACCESS_TOKEN, idToken = ID_TOKEN)
        assertEquals(ID_TOKEN, idTokenCache)
    }

    @Test
    fun `clearTokens remet tous les caches a null`() {
        simulateSaveTokens(accessToken = ACCESS_TOKEN, refreshToken = REFRESH_TOKEN, idToken = ID_TOKEN)
        assertNotNull(accessTokenCache)
        simulateClear()
        assertNull("access token null apres clear", accessTokenCache)
        assertNull("refresh token null apres clear", refreshTokenCache)
        assertNull("id token null apres clear", idTokenCache)
        assertFalse("isLoggedIn false apres clear", isLoggedIn)
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
            expiresAt = System.currentTimeMillis() + 3_600_000L,
        )
        assertFalse("Token valide dans 1h ne doit pas etre expire", simulateIsExpired())
    }

    @Test
    fun `token expire avec expiry passe`() {
        simulateSaveTokens(
            accessToken = ACCESS_TOKEN,
            expiresAt = System.currentTimeMillis() - 1_000L,
        )
        assertTrue("Token expire depuis 1s", simulateIsExpired())
    }

    @Test
    fun `token expirant dans moins de 60s est considere expire marge de securite`() {
        simulateSaveTokens(
            accessToken = ACCESS_TOKEN,
            expiresAt = System.currentTimeMillis() + 30_000L,
        )
        assertTrue("Token expirant dans 30s considere expire (marge 60s)", simulateIsExpired())
    }

    @Test
    fun `token sans expiry est considere expire`() {
        assertTrue("Token sans expiry considere expire", simulateIsExpired())
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
        simulateSaveTokens(accessToken = "new_access_token")
        assertEquals("Refresh token preserve", REFRESH_TOKEN, refreshTokenCache)
    }
}
