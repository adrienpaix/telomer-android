package health.telomer.android.feature.nutrition.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import health.telomer.android.feature.nutrition.data.api.NutritionApi
import health.telomer.android.feature.nutrition.data.repository.NutritionRepositoryImpl
import health.telomer.android.feature.nutrition.domain.repository.NutritionRepository
import health.telomer.android.feature.nutrition.engine.FoodRecognitionEngine
import health.telomer.android.feature.nutrition.engine.MockFoodRecognitionEngine
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NutritionBindsModule {

    @Binds
    @Singleton
    abstract fun bindNutritionRepository(impl: NutritionRepositoryImpl): NutritionRepository

    @Binds
    @Singleton
    abstract fun bindFoodRecognitionEngine(impl: MockFoodRecognitionEngine): FoodRecognitionEngine
}

@Module
@InstallIn(SingletonComponent::class)
object NutritionProvidesModule {

    @Provides
    @Singleton
    fun provideNutritionApi(retrofit: Retrofit): NutritionApi {
        return retrofit.create(NutritionApi::class.java)
    }
}
