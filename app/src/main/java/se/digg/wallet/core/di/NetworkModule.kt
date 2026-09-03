// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.di

import android.annotation.SuppressLint
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
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.inject.Qualifier
import javax.inject.Singleton
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import se.digg.wallet.BuildConfig
import se.digg.wallet.core.designsystem.utils.getAppVersion
import se.digg.wallet.core.network.DeviceInfo
import se.digg.wallet.core.network.SessionManager
import se.digg.wallet.core.network.authPlugin
import se.digg.wallet.core.network.deviceInfoPlugin
import se.digg.wallet.core.network.dpopPlugin
import se.digg.wallet.core.services.OpenIdNetworkService
import se.digg.wallet.core.storage.user.UserDao
import se.wallet.client.gateway.client.PublicAuthSessionChallengeClient
import se.wallet.client.gateway.client.PublicAuthSessionResponseClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BaseHttpClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GatewayHttpClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class UnsafeHttpClient

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
    fun provideHttpClient(deviceInfo: DeviceInfo): HttpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(networkJson)
            json(networkJson, contentType = problemJsonContentType)
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 20_000
            connectTimeoutMillis = 20_000
        }

        if (BuildConfig.DEBUG) {
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
        userDao: UserDao,
    ): SessionManager {
        val client = base.config {
            defaultRequest {
                url {
                    protocol = URLProtocol.HTTPS
                    host = BuildConfig.BASE_URL
                }
                header("X-API-KEY", BuildConfig.API_KEY)
            }
        }
        return SessionManager(
            challengeClient = PublicAuthSessionChallengeClient(client),
            validateClient = PublicAuthSessionResponseClient(client),
            userDao = userDao,
        )
    }

    @Provides
    @Singleton
    @GatewayHttpClient
    fun provideGatewayClient(
        @BaseHttpClient base: HttpClient,
        sessionManager: SessionManager,
    ): HttpClient {
        val client = base.config {
            defaultRequest {
                url {
                    protocol = URLProtocol.HTTPS
                    host = BuildConfig.BASE_URL
                }
                header("X-API-KEY", BuildConfig.API_KEY)
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
