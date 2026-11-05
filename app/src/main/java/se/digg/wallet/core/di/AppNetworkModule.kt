// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import se.digg.wallet.BuildConfig
import se.digg.wallet.core.network.NetworkConfig

@Module
@InstallIn(SingletonComponent::class)
object AppNetworkModule {

    @Provides
    @Singleton
    fun provideNetworkConfig(): NetworkConfig = NetworkConfig(
        baseUrl = BuildConfig.BASE_URL,
        apiKey = BuildConfig.API_KEY,
        isDebug = BuildConfig.DEBUG,
    )
}
