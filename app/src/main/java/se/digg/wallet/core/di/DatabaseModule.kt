package se.digg.wallet.core.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import se.digg.wallet.core.storage.user.AppDatabase
import se.digg.wallet.core.storage.user.UserDao

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase = Room
        .databaseBuilder(
            context,
            AppDatabase::class.java,
            "app-db",
        ).build()

    @Provides
    fun provideUser(database: AppDatabase): UserDao = database.userDao()
}
