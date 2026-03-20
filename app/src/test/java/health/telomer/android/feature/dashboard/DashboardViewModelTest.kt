package health.telomer.android.feature.dashboard

import health.telomer.android.core.data.api.models.AppointmentResponse
import health.telomer.android.core.data.api.models.ConversationResponse
import health.telomer.android.core.data.api.models.PatientProfile
import health.telomer.android.core.data.api.models.HealthOSDashboardResponse
import health.telomer.android.feature.dashboard.data.DashboardRepository
import io.mockk.coEvery
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
    private lateinit var repository: DashboardRepository
    private lateinit var viewModel: DashboardViewModel

    private val fakeProfile = PatientProfile(
        firstName = "Alice",
        lastName = "Dupont",
        email = "alice@telomer.health",
    )

    private val fakeDashboard = HealthOSDashboardResponse(
        patientId = "test-id",
        globalScore = 78.5,
        globalConfidence = null,
        inflammation = null,
        pillars = emptyList(),
        computedAt = null,
    )

    private val futureDate = "2099-12-31T10:00:00Z"
    private val pastDate = "2000-01-01T10:00:00Z"

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadDashboard success - firstName correct`() = runTest {
        coEvery { repository.getProfile() } returns fakeProfile
        coEvery { repository.getUpcomingAppointments() } returns emptyList()
        coEvery { repository.getConversations() } returns emptyList()
        coEvery { repository.getHealthOSDashboard() } returns fakeDashboard

        viewModel = DashboardViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Alice", state.firstName)
        assertEquals(78.5, state.healthOSScore)
        assertNull(state.error)
    }

    @Test
    fun `loadDashboard failure - erreur affichee`() = runTest {
        coEvery { repository.getProfile() } throws RuntimeException("Serveur indisponible")

        viewModel = DashboardViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.error)
        assertTrue(state.error!!.contains("Serveur indisponible"))
    }

    @Test
    fun `unreadMessages - somme correcte`() = runTest {
        coEvery { repository.getProfile() } returns fakeProfile
        coEvery { repository.getUpcomingAppointments() } returns emptyList()
        coEvery { repository.getConversations() } returns listOf(
            ConversationResponse(userId = "u1", userName = "Dr Martin", unreadCount = 3),
            ConversationResponse(userId = "u2", userName = "Dr Dupont", unreadCount = 5),
        )
        coEvery { repository.getHealthOSDashboard() } returns null

        viewModel = DashboardViewModel(repository)
        advanceUntilIdle()

        assertEquals(8, viewModel.uiState.value.unreadMessages)
    }

    @Test
    fun `nextAppointment - premier rendez-vous a venir selectionne`() = runTest {
        val nearFuture = AppointmentResponse(id = "a_near", scheduledAt = "2099-06-15T10:00:00Z", status = "confirmed")
        val farFuture = AppointmentResponse(id = "a_far", scheduledAt = "2099-12-31T10:00:00Z", status = "confirmed")
        val pastAppt = AppointmentResponse(id = "a_past", scheduledAt = pastDate, status = "confirmed")

        coEvery { repository.getProfile() } returns fakeProfile
        coEvery { repository.getUpcomingAppointments() } returns listOf(farFuture, nearFuture, pastAppt)
        coEvery { repository.getConversations() } returns emptyList()
        coEvery { repository.getHealthOSDashboard() } returns null

        viewModel = DashboardViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.nextAppointment)
        assertEquals("a_near", state.nextAppointment!!.id)
    }

    @Test
    fun `rendez-vous annules ignores`() = runTest {
        val cancelled = AppointmentResponse(id = "a1", scheduledAt = futureDate, status = "cancelled")

        coEvery { repository.getProfile() } returns fakeProfile
        coEvery { repository.getUpcomingAppointments() } returns listOf(cancelled)
        coEvery { repository.getConversations() } returns emptyList()
        coEvery { repository.getHealthOSDashboard() } returns null

        viewModel = DashboardViewModel(repository)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.nextAppointment)
    }

    @Test
    fun `healthOSScore null si API health-os indisponible`() = runTest {
        coEvery { repository.getProfile() } returns fakeProfile
        coEvery { repository.getUpcomingAppointments() } returns emptyList()
        coEvery { repository.getConversations() } returns emptyList()
        coEvery { repository.getHealthOSDashboard() } returns null

        viewModel = DashboardViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.healthOSScore)
        assertNull(state.error)
    }
}
