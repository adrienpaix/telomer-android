package health.telomer.android.feature.healthos

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import com.google.common.truth.Truth.assertThat
import health.telomer.android.core.data.api.TelomerApi
import health.telomer.android.core.data.api.models.*

@OptIn(ExperimentalCoroutinesApi::class)
class HealthOSViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var api: TelomerApi
    private lateinit var viewModel: HealthOSViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        api = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadDashboard success sets dashboard in state`() = runTest {
        val mockDashboard = HealthOSDashboardResponse(
            patientId = "test-id",
            globalScore = 75.5,
            globalConfidence = null,
            inflammation = null,
            pillars = emptyList(),
            computedAt = null,
        )
        coEvery { api.getHealthOSDashboard() } returns mockDashboard

        viewModel = HealthOSViewModel(api)
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.value.dashboard).isEqualTo(mockDashboard)
        assertThat(viewModel.uiState.value.isLoading).isFalse()
        assertThat(viewModel.uiState.value.error).isNull()
    }

    @Test
    fun `loadDashboard failure sets error in state`() = runTest {
        coEvery { api.getHealthOSDashboard() } throws RuntimeException("Network error")

        viewModel = HealthOSViewModel(api)
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.value.error).isNotNull()
        assertThat(viewModel.uiState.value.isLoading).isFalse()
    }

    @Test
    fun `globalScore as Double is not truncated`() = runTest {
        val mockDashboard = HealthOSDashboardResponse(
            patientId = "test-id",
            globalScore = 63.78,
            globalConfidence = null,
            inflammation = null,
            pillars = emptyList(),
            computedAt = null,
        )
        coEvery { api.getHealthOSDashboard() } returns mockDashboard

        viewModel = HealthOSViewModel(api)
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.value.dashboard?.globalScore).isEqualTo(63.78)
    }

    @Test
    fun `selectPillar failure sets error in state`() = runTest {
        val mockDashboard = HealthOSDashboardResponse(
            patientId = "test-id",
            globalScore = 70.0,
            globalConfidence = null,
            inflammation = null,
            pillars = emptyList(),
            computedAt = null,
        )
        coEvery { api.getHealthOSDashboard() } returns mockDashboard
        coEvery { api.getHealthOSPillar("cardiovascular") } throws RuntimeException("Server error")

        viewModel = HealthOSViewModel(api)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectPillar("cardiovascular")
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.value.error).isNotNull()
        assertThat(viewModel.uiState.value.selectedPillarCode).isNull()
    }
}
