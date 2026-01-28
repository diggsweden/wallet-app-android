package se.digg.wallet.core.di

import android.annotation.SuppressLint
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import se.digg.wallet.core.network.SessionManager
import se.digg.wallet.core.network.authPlugin
import se.digg.wallet.core.services.OpenIdNetworkService
import se.digg.wallet.core.storage.user.UserDao
import se.wallet.client.gateway.client.PublicAuthSessionChallengeClient
import se.wallet.client.gateway.client.PublicAuthSessionResponseClient
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.inject.Qualifier
import javax.inject.Singleton
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BaseHttpClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GatewayHttpClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class UnsafeHttpClient

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    @BaseHttpClient
    fun provideHttpClient(): HttpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                }
            )
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 20_000
            connectTimeoutMillis = 20_000
        }

        install(Logging)
    }

    @Provides
    @Singleton
    @UnsafeHttpClient
    @SuppressLint("TrustAllX509TrustManager")
    fun provideUnsafeHttpClient(): HttpClient {
        val trustAllCerts = arrayOf<TrustManager>(
            @SuppressLint("CustomX509TrustManager")
            object : X509TrustManager {
                override fun checkClientTrusted(
                    chain: Array<out X509Certificate>?,
                    authType: String?
                ) {
                }

                override fun checkServerTrusted(
                    chain: Array<out X509Certificate>?,
                    authType: String?
                ) {
                }

                override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
            }
        )

        val trustManager = trustAllCerts[0] as X509TrustManager

        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, trustAllCerts, SecureRandom())
        }

        val unsafeOkHttp = OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .hostnameVerifier { _, _ -> true }
            .build()

        return HttpClient(OkHttp) {
            engine {
                preconfigured = unsafeOkHttp
            }
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                    }
                )
            }
        }
    }

    @Provides
    @Singleton
    @GatewayHttpClient
    fun provideGatewayClient(
        @BaseHttpClient
        base: HttpClient,
        userDao: UserDao
    ): HttpClient {
        val client = base.config {
            defaultRequest {
                url {
                    protocol = URLProtocol.HTTPS
                    host = "wallet.sandbox.digg.se/api"
                }
                header("X-API-KEY", "my_secret_key")
            }
        }

        val sessionManager = SessionManager(
            challengeClient = PublicAuthSessionChallengeClient(client),
            validateClient = PublicAuthSessionResponseClient(client),
            userDao = userDao
        )

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