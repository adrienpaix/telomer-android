package health.telomer.android.feature.healthconnect.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import health.telomer.android.feature.healthconnect.domain.HealthMetric
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

private val Context.syncStore by preferencesDataStore(name = "health_sync_prefs")

@Singleton
class HealthConnectSync @Inject constructor(
    @ApplicationContext private val context: Context,
    private val manager: HealthConnectManager,
    private val api: HealthConnectApi,
) {
    companion object {
        private val LAST_SYNC = longPreferencesKey("last_sync_epoch")
        private val ISO = DateTimeFormatter.ISO_INSTANT
        private const val BATCH_SIZE = 100
    }

    // ── Last sync timestamp ──────────────────────────────────────

    suspend fun getLastSyncEpoch(): Long? {
        return context.syncStore.data.map { prefs ->
            prefs[LAST_SYNC]
        }.first()
    }

    private suspend fun setLastSyncEpoch(epoch: Long) {
        context.syncStore.edit { it[LAST_SYNC] = epoch }
    }

    // ── Sync ─────────────────────────────────────────────────────

    /**
     * Read metrics from Health Connect and push them to the API.
     * @param days How many days to sync (default 1 = last 24 h).
     * @return total synced count (from server response).
     */
    suspend fun sync(days: Int = 1): SyncResult {
        val metrics = manager.readLastDays(days)
        if (metrics.isEmpty()) return SyncResult(0, 0)

        var totalSynced = 0
        var totalDuplicates = 0

        metrics.chunked(BATCH_SIZE).forEach { batch ->
            val payload = batch.map { it.toPayload() }
            val resp = api.syncMetrics(MetricsSyncRequest(payload))
            totalSynced += resp.synced
            totalDuplicates += resp.duplicates
        }

        setLastSyncEpoch(Instant.now().epochSecond)
        return SyncResult(totalSynced, totalDuplicates)
    }

    /**
     * Auto-sync: sync the last 24 h if last sync was more than 1 h ago (or never).
     */
    suspend fun autoSyncIfNeeded(): SyncResult? {
        val lastEpoch = getLastSyncEpoch()
        val oneHourAgo = Instant.now().epochSecond - 3600
        if (lastEpoch != null && lastEpoch > oneHourAgo) return null
        return sync(days = 1)
    }

    private fun HealthMetric.toPayload() = MetricPayload(
        type = type.apiName,
        value = value,
        unit = unit,
        recordedAt = ISO.format(recordedAt.atOffset(ZoneOffset.UTC)),
    )
}

data class SyncResult(
    val synced: Int,
    val duplicates: Int,
)
