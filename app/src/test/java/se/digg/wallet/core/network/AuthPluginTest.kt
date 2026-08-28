// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthPluginTest {

    private val sessionManager = mockk<SessionManager>(relaxed = true).also {
        coEvery { it.getToken() } returns "session-token"
    }
    private val requests = mutableListOf<HttpRequestData>()

    private fun client(status: HttpStatusCode = HttpStatusCode.OK): HttpClient = HttpClient(
        MockEngine { request ->
            requests += request
            respond(content = "{}", status = status)
        },
    ) {
        expectSuccess = false
        install(authPlugin) { this.sessionManager = this@AuthPluginTest.sessionManager }
    }

    @Test
    fun `an ordinary request carries the session header`() = runTest {
        client().post("https://gateway.example.test/v0/accounts/acc-1/wallet-keys")

        assertEquals("session-token", requests.single().headers["session"])
    }

    @Test
    fun `account creation is exempt from the session header`() = runTest {
        client().post("https://gateway.example.test/v0/accounts")

        assertNull(requests.single().headers["session"])
        verify(exactly = 0) { sessionManager.reset() }
    }

    @Test
    fun `a forbidden response invalidates the cached session`() = runTest {
        client(HttpStatusCode.Forbidden).post("https://gateway.example.test/hsm/v0/requests")

        verify(exactly = 1) { sessionManager.reset() }
    }

    @Test
    fun `a successful response keeps the cached session`() = runTest {
        client(HttpStatusCode.OK).post("https://gateway.example.test/hsm/v0/requests")

        verify(exactly = 0) { sessionManager.reset() }
    }

    @Test
    fun `an unauthorized response keeps the cached session`() = runTest {
        client(HttpStatusCode.Unauthorized).post("https://gateway.example.test/hsm/v0/requests")

        verify(exactly = 0) { sessionManager.reset() }
    }
}
