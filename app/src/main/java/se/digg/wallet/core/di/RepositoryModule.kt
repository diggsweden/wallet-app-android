package se.digg.wallet.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import se.digg.wallet.core.network.ApiService
import se.digg.wallet.core.storage.user.UserDao
import se.digg.wallet.data.UserRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideUserRepository(
        apiService: ApiService,
        userDao: UserDao,
        @GatewayHttpClient gatewayClient: HttpClient
    ): UserRepository =
        UserRepository(apiService = apiService, userDao = userDao, gatewayClient = gatewayClient)
}