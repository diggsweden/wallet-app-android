// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.network.di

import android.content.Context
import android.os.Build
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.ANDROID
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.json.json
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import se.digg.wallet.core.common.utils.getAppVersion
import se.digg.wallet.core.network.DeviceAuthenticator
import se.digg.wallet.core.network.DeviceInfo
import se.digg.wallet.core.network.NetworkConfig
import se.digg.wallet.core.network.OpenIdNetworkService
import se.digg.wallet.core.network.SessionManager
import se.digg.wallet.core.network.authPlugin
import se.digg.wallet.core.network.deviceInfoPlugin
import se.digg.wallet.core.network.dpopPlugin
import se.wallet.client.gateway.client.PublicAuthSessionChallengeClient
import se.wallet.client.gateway.client.PublicAuthSessionResponseClient

private val networkJson = Json {
    ignoreUnknownKeys = true
}

private val problemJsonContentType = ContentType("application", "problem+json")

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    @BaseHttpClient
    fun provideHttpClient(networkConfig: NetworkConfig, deviceInfo: DeviceInfo): HttpClient =
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(networkJson)
                json(networkJson, contentType = problemJsonContentType)
            }

            install(HttpTimeout) {
                requestTimeoutMillis = 20_000
                connectTimeoutMillis = 20_000
            }

            if (networkConfig.isDebug) {
                install(Logging) {
                    logger = Logger.ANDROID
                    level = LogLevel.HEADERS
                }
            }

            install(dpopPlugin)
            install(deviceInfoPlugin) {
                this.deviceInfo = deviceInfo
            }
        }

    @Provides
    @Singleton
    fun provideDeviceInfo(@ApplicationContext context: Context): DeviceInfo {
        val appVersion = getAppVersion(context)
        return DeviceInfo(
            os = "Android",
            osVersion = Build.VERSION.RELEASE ?: "?",
            model = Build.MODEL,
            appVersion = "${appVersion.versionName} (${appVersion.versionCode})",
        )
    }

    @Provides
    @Singleton
    fun provideSessionManager(
        @BaseHttpClient base: HttpClient,
        networkConfig: NetworkConfig,
        authenticator: DeviceAuthenticator,
    ): SessionManager {
        val client = base.config {
            defaultRequest {
                url {
                    protocol = URLProtocol.HTTPS
                    host = networkConfig.baseUrl
                }
                header("X-API-KEY", networkConfig.apiKey)
            }
        }
        return SessionManager(
            challengeClient = PublicAuthSessionChallengeClient(client),
            validateClient = PublicAuthSessionResponseClient(client),
            authenticator = authenticator,
        )
    }

    @Provides
    @Singleton
    @GatewayHttpClient
    fun provideGatewayClient(
        @BaseHttpClient base: HttpClient,
        networkConfig: NetworkConfig,
        sessionManager: SessionManager,
    ): HttpClient {
        val client = base.config {
            defaultRequest {
                url {
                    protocol = URLProtocol.HTTPS
                    host = networkConfig.baseUrl
                }
                header("X-API-KEY", networkConfig.apiKey)
            }
        }
        return client.config {
            install(authPlugin) {
                this.sessionManager = sessionManager
            }
        }
    }

    @Provides
    @Singleton
    fun provideOpenIdNetworkService(@BaseHttpClient base: HttpClient) = OpenIdNetworkService(base)
}
