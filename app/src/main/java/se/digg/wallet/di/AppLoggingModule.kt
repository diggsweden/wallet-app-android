// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.di

import android.util.Log
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import se.digg.wallet.BuildConfig
import se.digg.wallet.core.logging.RemoteLoggingConfig

@Module
@InstallIn(SingletonComponent::class)
object AppLoggingModule {

    @Provides
    @Singleton
    fun provideRemoteLoggingConfig(): RemoteLoggingConfig = RemoteLoggingConfig(
        enabled = true,
        minPriority = if (BuildConfig.DEBUG) Log.DEBUG else Log.WARN,
    )
}
