package health.telomer.android.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
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
}
