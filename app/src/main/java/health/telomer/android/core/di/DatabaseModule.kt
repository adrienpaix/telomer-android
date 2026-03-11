package health.telomer.android.core.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import health.telomer.android.core.data.local.ActionPlanCheckDao
import health.telomer.android.core.data.local.TelomerDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TelomerDatabase =
        Room.databaseBuilder(context, TelomerDatabase::class.java, "telomer-db").build()

    @Provides
    fun provideActionPlanCheckDao(db: TelomerDatabase): ActionPlanCheckDao =
        db.actionPlanCheckDao()
}
