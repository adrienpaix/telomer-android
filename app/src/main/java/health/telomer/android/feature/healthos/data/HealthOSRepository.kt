package health.telomer.android.feature.healthos.data

import health.telomer.android.core.data.api.TelomerApi
import health.telomer.android.core.data.api.models.HealthOSDashboardResponse
import health.telomer.android.core.data.api.models.PillarDetailResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthOSRepository @Inject constructor(
    private val api: TelomerApi,
) {
    suspend fun getDashboard(): HealthOSDashboardResponse = api.getHealthOSDashboard()
    suspend fun getPillar(code: String): PillarDetailResponse = api.getHealthOSPillar(code)
}
