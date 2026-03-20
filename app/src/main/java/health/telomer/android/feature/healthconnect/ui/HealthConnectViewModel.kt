package health.telomer.android.feature.healthconnect.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import health.telomer.android.feature.healthconnect.data.HealthConnectAvailability
import health.telomer.android.feature.healthconnect.data.HealthConnectManager
import health.telomer.android.feature.healthconnect.data.HealthConnectSync
import health.telomer.android.feature.healthconnect.data.SyncResult
import health.telomer.android.feature.healthconnect.domain.HealthMetric
import health.telomer.android.feature.healthconnect.domain.MetricType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

data class HealthConnectUiState(
    val availability: HealthConnectAvailability = HealthConnectAvailability.NOT_SUPPORTED,
    val permissionsGranted: Boolean = false,
    val todayMetrics: Map<MetricType, List<HealthMetric>> = emptyMap(),
    val weekMetrics: Map<MetricType, List<HealthMetric>> = emptyMap(),
    val isSyncing: Boolean = false,
    val lastSyncEpoch: Long? = null,
    val syncResult: SyncResult? = null,
    val backendSyncCount: Int? = null,
    val backendSyncError: String? = null,
    val error: String? = null,
    val userAge: Int = HealthConnectManager.DEFAULT_AGE,
    val sleepScore: Int = 0,
    val recoveryScore: Int = 0,
    val strainScore: Double = 0.0,
    val sleepDebtHours: Double = 0.0,
)

@HiltViewModel
class HealthConnectViewModel @Inject constructor(
    private val manager: HealthConnectManager,
    private val sync: HealthConnectSync,
) : ViewModel() {

    private val _state = MutableStateFlow(HealthConnectUiState())
    val state: StateFlow<HealthConnectUiState> = _state.asStateFlow()

    init {
        checkAvailability()
    }

    fun checkAvailability() {
        val availability = manager.checkAvailability()
        _state.update { it.copy(availability = availability) }
        if (availability == HealthConnectAvailability.AVAILABLE) {
            viewModelScope.launch { checkPermissionsAndLoad() }
        }
    }

    fun onPermissionsResult(granted: Set<String>) {
        val allGranted = HealthConnectManager.PERMISSIONS.all { it in granted }
        _state.update { it.copy(permissionsGranted = allGranted) }
        if (allGranted) {
            loadData()
            autoSync()
        }
    }

    private suspend fun checkPermissionsAndLoad() {
        val granted = manager.hasAllPermissions()
        _state.update { it.copy(permissionsGranted = granted) }
        if (granted) {
            loadData()
            autoSync()
        }
    }

    fun loadData() {
        viewModelScope.launch {
            try {
                val age = _state.value.userAge
                val today = manager.readToday(age)
                val week = manager.readLastDays(7, age)
                val lastSync = sync.getLastSyncEpoch()
                _state.update {
                    it.copy(
                        todayMetrics = today.groupBy { m -> m.type },
                        weekMetrics = week.groupBy { m -> m.type },
                        lastSyncEpoch = lastSync,
                        error = null,
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Erreur de lecture : ${e.message}") }
            }
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            _state.update { it.copy(isSyncing = true, error = null, backendSyncCount = null, backendSyncError = null) }
            try {
                val result = sync.sync(days = 7, userAge = _state.value.userAge)
                // Also sync to new backend bulk endpoint POST /me/health-metrics/bulk
                val backendCount = try {
                    sync.syncToBackend(userAge = _state.value.userAge)
                } catch (e: Exception) {
                    _state.update { it.copy(backendSyncError = "Backend: ${e.message}") }
                    -1
                }
                _state.update {
                    it.copy(
                        isSyncing = false,
                        syncResult = result,
                        lastSyncEpoch = Instant.now().epochSecond,
                        backendSyncCount = if (backendCount >= 0) backendCount else null,
                    )
                }
                loadData()
            } catch (e: Exception) {
                _state.update {
                    it.copy(isSyncing = false, error = "Erreur de sync : ${e.message}")
                }
            }
        }
    }

    private fun autoSync() {
        viewModelScope.launch {
            try {
                sync.autoSyncIfNeeded(_state.value.userAge)
                val lastSync = sync.getLastSyncEpoch()
                _state.update { it.copy(lastSyncEpoch = lastSync) }
            } catch (_: Exception) { /* silent */ }
        }
    }
}
