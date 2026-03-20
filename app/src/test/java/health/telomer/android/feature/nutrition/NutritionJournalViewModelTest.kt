package health.telomer.android.feature.nutrition

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import com.google.common.truth.Truth.assertThat
import health.telomer.android.feature.nutrition.domain.model.*
import health.telomer.android.feature.nutrition.domain.repository.NutritionRepository
import health.telomer.android.feature.nutrition.ui.journal.NutritionJournalViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class NutritionJournalViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: NutritionRepository
    private lateinit var viewModel: NutritionJournalViewModel

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
    fun `loadSummary success sets summary in state`() = runTest {
        val mockSummary = DailySummary(
            date = "2024-01-01",
            meals = emptyList(),
            totalCalories = 1800.0,
            totalProteins = 90.0,
            totalCarbs = 200.0,
            totalFats = 60.0,
        )
        coEvery { repository.getDailySummary(any()) } returns Result.success(mockSummary)

        viewModel = NutritionJournalViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.state.value.summary).isEqualTo(mockSummary)
        assertThat(viewModel.state.value.isLoading).isFalse()
        assertThat(viewModel.state.value.error).isNull()
    }

    @Test
    fun `loadSummary failure sets error in state`() = runTest {
        coEvery { repository.getDailySummary(any()) } returns Result.failure(RuntimeException("Network error"))

        viewModel = NutritionJournalViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.state.value.error).isNotNull()
        assertThat(viewModel.state.value.isLoading).isFalse()
    }

    @Test
    fun `loadSummary state is not loading after success`() = runTest {
        val mockSummary = DailySummary(
            date = "2024-01-01",
            meals = emptyList(),
            totalCalories = 2000.0,
            totalProteins = 100.0,
            totalCarbs = 250.0,
            totalFats = 70.0,
        )
        coEvery { repository.getDailySummary(any()) } returns Result.success(mockSummary)

        viewModel = NutritionJournalViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.state.value.isLoading).isFalse()
    }
}
