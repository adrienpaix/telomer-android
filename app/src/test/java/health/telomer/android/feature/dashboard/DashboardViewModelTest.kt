package health.telomer.android.feature.dashboard

import health.telomer.android.core.data.api.TelomerApi
import health.telomer.android.core.data.api.models.AppointmentResponse
import health.telomer.android.core.data.api.models.ConversationResponse
import health.telomer.android.core.data.api.models.PatientProfile
import health.telomer.android.core.data.api.models.HealthOSDashboardResponse
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var api: TelomerApi
    private lateinit var viewModel: DashboardViewModel

    private val fakeProfile = PatientProfile(
        firstName = "Alice",
        lastName = "Dupont",
        email = "alice@telomer.health",
    )

    private val futureDate = "2099-12-31T10:00:00Z"
    private val pastDate = "2000-01-01T10:00:00Z"

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        api = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadDashboard success - firstName correct`() = runTest {
        coEvery { api.getMyProfile() } returns fakeProfile
        coEvery { api.getMyAppointments() } returns emptyList()
        coEvery { api.getConversations() } returns emptyList()
        coEvery { api.getHealthOSDashboard() } returns mockk { every { globalScore } returns 78.5 }

        viewModel = DashboardViewModel(api)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Alice", state.firstName)
        assertEquals(78.5, state.healthOSScore)
        assertNull(state.error)
    }

    @Test
    fun `loadDashboard failure - erreur affichee`() = runTest {
        coEvery { api.getMyProfile() } throws RuntimeException("Serveur indisponible")

        viewModel = DashboardViewModel(api)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.error)
        assertTrue(state.error!!.contains("Serveur indisponible"))
    }

    @Test
    fun `unreadMessages - somme correcte`() = runTest {
        coEvery { api.getMyProfile() } returns fakeProfile
        coEvery { api.getMyAppointments() } returns emptyList()
        coEvery { api.getConversations() } returns listOf(
            ConversationResponse(userId = "u1", userName = "Dr Martin", unreadCount = 3),
            ConversationResponse(userId = "u2", userName = "Dr Dupont", unreadCount = 5),
        )
        coEvery { api.getHealthOSDashboard() } throws RuntimeException("pas dispo")

        viewModel = DashboardViewModel(api)
        advanceUntilIdle()

        assertEquals(8, viewModel.uiState.value.unreadMessages)
    }

    @Test
    fun `nextAppointment - premier rendez-vous a venir selectionne`() = runTest {
        val nearFuture = AppointmentResponse(id = "a_near", scheduledAt = "2099-06-15T10:00:00Z", status = "confirmed")
        val farFuture = AppointmentResponse(id = "a_far", scheduledAt = "2099-12-31T10:00:00Z", status = "confirmed")
        val pastAppt = AppointmentResponse(id = "a_past", scheduledAt = pastDate, status = "confirmed")

        coEvery { api.getMyProfile() } returns fakeProfile
        coEvery { api.getMyAppointments() } returns listOf(farFuture, nearFuture, pastAppt)
        coEvery { api.getConversations() } returns emptyList()
        coEvery { api.getHealthOSDashboard() } throws RuntimeException("pas dispo")

        viewModel = DashboardViewModel(api)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.nextAppointment)
        assertEquals("Le RDV le plus proche dans le futur", "a_near", state.nextAppointment!!.id)
    }

    @Test
    fun `rendez-vous annules ignores`() = runTest {
        val cancelled = AppointmentResponse(id = "a1", scheduledAt = futureDate, status = "cancelled")

        coEvery { api.getMyProfile() } returns fakeProfile
        coEvery { api.getMyAppointments() } returns listOf(cancelled)
        coEvery { api.getConversations() } returns emptyList()
        coEvery { api.getHealthOSDashboard() } throws RuntimeException("pas dispo")

        viewModel = DashboardViewModel(api)
        advanceUntilIdle()

        assertNull("RDV annule ignore", viewModel.uiState.value.nextAppointment)
    }

    @Test
    fun `healthOSScore null si API health-os indisponible`() = runTest {
        coEvery { api.getMyProfile() } returns fakeProfile
        coEvery { api.getMyAppointments() } returns emptyList()
        coEvery { api.getConversations() } returns emptyList()
        coEvery { api.getHealthOSDashboard() } throws RuntimeException("Service indisponible")

        viewModel = DashboardViewModel(api)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.healthOSScore)
        // Pas d'erreur globale car health-os est catch individuellement
        assertNull(state.error)
    }
}
