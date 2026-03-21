package health.telomer.android.feature.healthconnect.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import health.telomer.android.feature.healthconnect.data.HealthConnectAvailability
import health.telomer.android.feature.healthconnect.data.HealthConnectManager
import health.telomer.android.feature.healthconnect.data.HealthConnectSync

@HiltWorker
class HealthConnectSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val manager: HealthConnectManager,
    private val sync: HealthConnectSync,
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val TAG = "HealthConnectSyncWorker"
        const val WORK_NAME = "health_connect_daily_sync"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting daily Health Connect sync")

        if (manager.checkAvailability() != HealthConnectAvailability.AVAILABLE) {
            Log.d(TAG, "Health Connect not available, skipping")
            return Result.success()
        }

        if (!manager.hasAllPermissions()) {
            Log.d(TAG, "Permissions not granted, skipping")
            return Result.success()
        }

        return try {
            val result = sync.sync(days = 1, userAge = HealthConnectManager.DEFAULT_AGE)
            Log.d(TAG, "Sync completed: ${result.synced} metrics synced")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
