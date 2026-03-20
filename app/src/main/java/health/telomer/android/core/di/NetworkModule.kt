package health.telomer.android.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import health.telomer.android.BuildConfig
import health.telomer.android.auth.AuthManager
import health.telomer.android.core.data.api.TelomerApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(authManager: AuthManager): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .certificatePinner(
                CertificatePinner.Builder()
                    .add("api.telomer.health", "sha256/gN77EQZzOlzMaWaIGvpQILY1zUsTFsDfWshKc3R7hOg=")
                    .add("auth.telomer.health", "sha256/gN77EQZzOlzMaWaIGvpQILY1zUsTFsDfWshKc3R7hOg=")
                    .build()
            )
            .addInterceptor { chain ->
                // runBlocking on Dispatchers.IO — safe: OkHttp interceptors run on IO threads,
                // never on the main thread. Dispatchers.IO avoids starvation of the calling thread.
                val token = runBlocking(Dispatchers.IO) { authManager.getValidAccessToken() }
                val request = if (token != null) {
                    chain.request().newBuilder()
                        .header("Authorization", "Bearer $token")
                        .build()
                } else chain.request()
                chain.proceed(request)
            }
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                        else HttpLoggingInterceptor.Level.NONE
            })
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("${BuildConfig.API_BASE}/api/v1/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideTelomerApi(retrofit: Retrofit): TelomerApi {
        return retrofit.create(TelomerApi::class.java)
    }
}
