package health.telomer.android.feature.healthos

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import health.telomer.android.core.data.api.models.*
import health.telomer.android.feature.healthos.data.HealthOSRepository

@OptIn(ExperimentalCoroutinesApi::class)
class HealthOSViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: HealthOSRepository
    private lateinit var viewModel: HealthOSViewModel

    private val mockDashboard = HealthOSDashboardResponse(
        patientId = "test-id",
        globalScore = 75.5,
        globalConfidence = null,
        inflammation = null,
        pillars = emptyList(),
        computedAt = null,
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadDashboard success sets dashboard in state`() = runTest {
        coEvery { repository.getDashboard() } returns mockDashboard

        viewModel = HealthOSViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(mockDashboard, state.dashboard)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `loadDashboard failure sets error in state`() = runTest {
        coEvery { repository.getDashboard() } throws RuntimeException("Network error")

        viewModel = HealthOSViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.error)
        assertFalse(state.isLoading)
    }

    @Test
    fun `globalScore as Double is not truncated`() = runTest {
        val dashboardWith6378 = HealthOSDashboardResponse(
            patientId = "test-id",
            globalScore = 63.78,
            globalConfidence = null,
            inflammation = null,
            pillars = emptyList(),
            computedAt = null,
        )
        coEvery { repository.getDashboard() } returns dashboardWith6378

        viewModel = HealthOSViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(63.78, viewModel.uiState.value.dashboard?.globalScore)
    }

    @Test
    fun `selectPillar failure sets error in state`() = runTest {
        coEvery { repository.getDashboard() } returns mockDashboard
        coEvery { repository.getPillar("cardiovascular") } throws RuntimeException("Server error")

        viewModel = HealthOSViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectPillar("cardiovascular")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.error)
        assertNull(state.selectedPillarCode)
    }
}
