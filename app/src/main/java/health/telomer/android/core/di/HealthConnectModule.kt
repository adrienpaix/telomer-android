package health.telomer.android.core.di

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import health.telomer.android.feature.healthconnect.data.HealthConnectApi
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HealthConnectModule {

    @Provides
    @Singleton
    fun provideHealthConnectApi(retrofit: Retrofit): HealthConnectApi {
        return retrofit.create(HealthConnectApi::class.java)
    }

    /**
     * Provides HealthConnectClient for injection where needed.
     * Note: HealthConnectManager lazy-creates its own client on checkAvailability(),
     * but this binding allows direct injection if needed.
     */
    @Provides
    @Singleton
    fun provideHealthConnectClient(@ApplicationContext context: Context): HealthConnectClient {
        return HealthConnectClient.getOrCreate(context)
    }
}
