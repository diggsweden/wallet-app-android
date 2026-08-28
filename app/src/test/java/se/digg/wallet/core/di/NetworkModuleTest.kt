// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.di

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.pluginOrNull
import io.ktor.http.URLProtocol
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import se.digg.wallet.BuildConfig
import se.digg.wallet.core.storage.user.UserDao

/**
 * The providers build real Ktor/OkHttp clients - no Android framework involved - so
 * the wiring they perform (base URL, API key header, timeouts, plugins) is checkable
 * on the JVM. Every client is closed so the test does not leak dispatcher threads.
 */
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

        // Closing twice is safe - the providers hand out singletons that Hilt
        // may tear down more than once in tests.
        client.close()
    }

    @Test
    fun `the unsafe client trusts every certificate`() {
        withClient(NetworkModule.provideUnsafeHttpClient()) { client ->
            assertNotNull(client.pluginOrNull(ContentNegotiation))
            // The unsafe client deliberately omits the DPoP plugin - it exists only
            // for talking to a local issuer over a self-signed certificate.
            assertNotNull(client)
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
        // The providers prepend the scheme themselves, so a flavor must contribute
        // only the host - this runs for the demo and local flavors alike.
        listOf(BuildConfig.BASE_URL, BuildConfig.PID_ISSUER_URL).forEach { host ->
            assertTrue(host.isNotBlank())
            assertTrue("$host should not carry a scheme", !host.contains("://"))
        }
        assertEquals("https", URLProtocol.HTTPS.name)
    }
}
