package health.telomer.android.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import health.telomer.android.core.data.api.models.AppointmentResponse
import health.telomer.android.feature.dashboard.data.DashboardRepository
import health.telomer.android.feature.healthconnect.data.HealthConnectAvailability
import health.telomer.android.feature.healthconnect.data.HealthConnectManager
import health.telomer.android.feature.healthconnect.domain.MetricType
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = true,
    val firstName: String = "",
    val nextAppointment: AppointmentResponse? = null,
    val unreadMessages: Int = 0,
    val questionnaireStatus: String? = null,
    val error: String? = null,
    val healthOSScore: Double? = null,
    val sleepScore: Int = 0,
    val recoveryScore: Int = 0,
    val strainScore: Double = 0.0,
    val sleepDebtHours: Double = 0.0,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: DashboardRepository,
    private val healthConnectManager: HealthConnectManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                coroutineScope {
                    val profileDeferred = async { repository.getProfile() }
                    val apptsDeferred = async { repository.getUpcomingAppointments() ?: emptyList() }
                    val messagesDeferred = async { repository.getConversations() ?: emptyList() }
                    val healthDeferred = async { repository.getHealthOSDashboard() }

                    val profile = profileDeferred.await()
                    val appointments = apptsDeferred.await()
                    val conversations = messagesDeferred.await()
                    val health = healthDeferred.await()

                    val now = java.time.Instant.now().toString()
                    val nextAppt = appointments
                        .filter { it.status != "cancelled" && it.scheduledAt >= now }
                        .minByOrNull { it.scheduledAt }

                    val unread = conversations.sumOf { it.unreadCount }

                    _uiState.value = DashboardUiState(
                        isLoading = false,
                        firstName = profile.firstName,
                        nextAppointment = nextAppt,
                        unreadMessages = unread,
                        questionnaireStatus = null,
                        healthOSScore = health?.globalScore,
                    )
                    // Load Health Connect scores
                    loadHealthConnectScores()
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Erreur de chargement",
                    )
                }
            }
        }
    }

    private fun loadHealthConnectScores() {
        viewModelScope.launch {
            try {
                val availability = healthConnectManager.checkAvailability()
                if (availability != HealthConnectAvailability.AVAILABLE) return@launch
                val hasPerms = healthConnectManager.hasAllPermissions()
                if (!hasPerms) return@launch

                val age = HealthConnectManager.DEFAULT_AGE
                val todayMetrics = healthConnectManager.readToday(age)
                val weekMetrics = healthConnectManager.readLastDays(7, age)

                val todayByType = todayMetrics.groupBy { it.type }
                val weekByType = weekMetrics.groupBy { it.type }

                val steps = todayByType[MetricType.STEPS]?.sumOf { it.value }?.toInt() ?: 0
                val activeCals = todayByType[MetricType.ACTIVE_CALORIES]?.sumOf { it.value }?.toInt() ?: 0
                val exerciseMin = todayByType[MetricType.EXERCISE]?.sumOf { it.value }?.toInt() ?: 0
                val sleepMin = todayByType[MetricType.SLEEP]?.sumOf { it.value }?.toInt() ?: 0

                val hrResting = weekByType[MetricType.RESTING_HEART_RATE]?.lastOrNull()?.value?.toInt()
                val lastHRV = weekByType[MetricType.HRV]?.lastOrNull()?.value
                val lastSpO2 = weekByType[MetricType.SPO2]?.lastOrNull()?.value

                val lastSleep = weekByType[MetricType.SLEEP]?.lastOrNull()
                val sleepLight = lastSleep?.metadata?.get("light_minutes")?.toInt() ?: 0
                val sleepDeep = lastSleep?.metadata?.get("deep_minutes")?.toInt() ?: 0
                val sleepRem = lastSleep?.metadata?.get("rem_minutes")?.toInt() ?: 0

                val zoneMinutes = listOf(
                    MetricType.HEART_ZONE_1, MetricType.HEART_ZONE_2,
                    MetricType.HEART_ZONE_3, MetricType.HEART_ZONE_4, MetricType.HEART_ZONE_5,
                ).map { zType -> weekByType[zType]?.sumOf { it.value }?.toInt() ?: 0 }

                val sleepScore = computeSleepScoreDash(sleepMin, sleepDeep, sleepRem, sleepLight)
                val recoveryScore = computeRecoveryScoreDash(hrResting, lastHRV, lastSpO2, sleepScore)
                val strainScore = computeStrainScoreDash(steps, activeCals, exerciseMin, zoneMinutes)
                val sleepDebtHours = computeSleepDebtDash(sleepMin)

                _uiState.update { it.copy(
                    sleepScore = sleepScore,
                    recoveryScore = recoveryScore,
                    strainScore = strainScore,
                    sleepDebtHours = sleepDebtHours,
                ) }
            } catch (_: Exception) {
                // Silently fail — Health Connect data is optional on dashboard
            }
        }
    }

    private fun computeSleepScoreDash(sleepMin: Int, deepMin: Int, remMin: Int, lightMin: Int): Int {
        if (sleepMin == 0) return 0
        val durationScore = ((sleepMin.toFloat() / 480) * 40).coerceAtMost(40f)
        val totalSleep = (deepMin + remMin + lightMin).coerceAtLeast(1)
        val deepPct = deepMin.toFloat() / totalSleep
        val remPct = remMin.toFloat() / totalSleep
        val deepScore = when {
            deepPct in 0.15f..0.25f -> 30f; deepPct in 0.10f..0.30f -> 20f; else -> 10f
        }
        val remScore = when {
            remPct in 0.20f..0.25f -> 30f; remPct in 0.15f..0.30f -> 20f; else -> 10f
        }
        return (durationScore + deepScore + remScore).toInt().coerceIn(0, 100)
    }

    private fun computeRecoveryScoreDash(hrResting: Int?, hrvMs: Double?, spo2: Double?, sleepScore: Int): Int {
        var score = 0f
        score += when { hrResting == null -> 15f; hrResting <= 55 -> 30f; hrResting in 56..65 -> 25f; hrResting in 66..75 -> 18f; else -> 10f }
        score += when { hrvMs == null -> 15f; hrvMs >= 80 -> 30f; hrvMs >= 50 -> 22f; hrvMs >= 30 -> 15f; else -> 8f }
        score += when { spo2 == null -> 5f; spo2 >= 97 -> 10f; spo2 >= 95 -> 7f; else -> 3f }
        score += (sleepScore * 0.3f)
        return score.toInt().coerceIn(0, 100)
    }

    private fun computeStrainScoreDash(steps: Int, activeCals: Int, exerciseMin: Int, zoneMinutes: List<Int>): Double {
        val zoneWeight = if (zoneMinutes.size >= 5) {
            zoneMinutes[0] * 0.1 + zoneMinutes[1] * 0.3 + zoneMinutes[2] * 0.6 + zoneMinutes[3] * 1.0 + zoneMinutes[4] * 1.5
        } else 0.0
        return ((steps / 1000.0).coerceAtMost(5.0) + (activeCals / 200.0).coerceAtMost(5.0) + (exerciseMin / 15.0).coerceAtMost(5.0) + (zoneWeight / 30.0).coerceAtMost(6.0)).coerceIn(0.0, 21.0)
    }

    private fun computeSleepDebtDash(sleepMinToday: Int): Double {
        return ((480 - sleepMinToday).coerceAtLeast(0)) / 60.0
    }
}
