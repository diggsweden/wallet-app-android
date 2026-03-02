// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import javax.inject.Singleton
import se.digg.wallet.core.storage.user.UserDao
import se.digg.wallet.data.UserRepository

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideUserRepository(
        userDao: UserDao,
        @GatewayHttpClient gatewayClient: HttpClient,
    ): UserRepository = UserRepository(userDao = userDao, gatewayClient = gatewayClient)
}
