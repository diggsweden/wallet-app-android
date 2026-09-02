// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.di

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttpConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.pluginOrNull
import io.mockk.mockk
import javax.net.ssl.SSLSession
import kotlinx.coroutines.job
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import se.digg.wallet.BuildConfig
import se.digg.wallet.core.storage.user.UserDao

class NetworkModuleTest {

    private fun <T> withClient(client: HttpClient, block: (HttpClient) -> T): T = client.use(block)

    @Test
    fun `the base client negotiates json and carries request timeouts`() {
        withClient(NetworkModule.provideHttpClient()) { client ->
            assertNotNull(client.pluginOrNull(ContentNegotiation))
            val timeout = client.pluginOrNull(HttpTimeout)
            assertNotNull(timeout)
        }
    }

    @Test
    fun `the base client releases its engine on close`() {
        val client = NetworkModule.provideHttpClient()

        client.close()

        assertTrue(!client.coroutineContext.job.isActive)
    }

    @Test
    fun `the unsafe client accepts any certificate chain and any hostname`() {
        withClient(NetworkModule.provideUnsafeHttpClient()) { client ->
            val okHttp = requireNotNull((client.engine.config as OkHttpConfig).preconfigured)

            val trustManager = requireNotNull(okHttp.x509TrustManager)
            assertEquals(0, trustManager.acceptedIssuers.size)
            // An empty chain is what a real verifier rejects first; this one accepts it.
            trustManager.checkServerTrusted(emptyArray(), "ECDHE_ECDSA")

            assertTrue(okHttp.hostnameVerifier.verify("not.the.right.host", mockk<SSLSession>()))
        }
    }

    @Test
    fun `the session manager is built over the gateway base url and api key`() {
        withClient(NetworkModule.provideHttpClient()) { base ->
            val sessionManager = NetworkModule.provideSessionManager(base, mockk<UserDao>())

            assertNotNull(sessionManager.challengeClient)
            assertNotNull(sessionManager.validateClient)
        }
    }

    @Test
    fun `the gateway client is derived from the base client`() {
        withClient(NetworkModule.provideHttpClient()) { base ->
            val sessionManager = NetworkModule.provideSessionManager(base, mockk<UserDao>())

            NetworkModule.provideGatewayClient(base, sessionManager).use { gateway ->
                assertTrue(gateway !== base)
                assertNotNull(gateway.pluginOrNull(ContentNegotiation))
            }
        }
    }

    @Test
    fun `the openid network service is built over the base client`() {
        withClient(NetworkModule.provideHttpClient()) { base ->
            assertNotNull(NetworkModule.provideOpenIdNetworkService(base))
        }
    }

    @Test
    fun `every flavor supplies a scheme-less gateway and issuer host`() {
        listOf(BuildConfig.BASE_URL, BuildConfig.PID_ISSUER_URL).forEach { host ->
            assertTrue(host.isNotBlank())
            assertTrue("$host should not carry a scheme", !host.contains("://"))
        }
    }
}
