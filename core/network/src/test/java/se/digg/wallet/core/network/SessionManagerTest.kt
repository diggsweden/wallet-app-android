// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import se.wallet.client.gateway.client.PublicAuthSessionChallengeClient
import se.wallet.client.gateway.client.PublicAuthSessionResponseClient

class SessionManagerTest {

    private class FakeDeviceAuthenticator(
        val accountId: String = "test-account",
        val deviceKeyId: String = "test-key-id",
        var signedJwt: String = "mock-signed-jwt",
    ) : DeviceAuthenticator {
        var signCallCount = 0

        override suspend fun getAccountId(): String = accountId

        override suspend fun getDeviceKeyId(): String = deviceKeyId

        override suspend fun signChallenge(nonce: String): String {
            signCallCount++
            return "$signedJwt-$nonce"
        }
    }

    private fun createClient(sessionCounter: () -> Int): HttpClient = HttpClient(
        MockEngine { request ->
            val path = request.url.encodedPath
            when {
                path.contains("/auth/session/challenge") -> {
                    respond(
                        content = """{"nonce":"nonce-${sessionCounter()}"}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(
                            HttpHeaders.ContentType,
                            ContentType.Application.Json.toString(),
                        ),
                    )
                }

                path.contains("/auth/session/response") -> {
                    respond(
                        content = """{"sessionId":"session-token-${sessionCounter()}"}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(
                            HttpHeaders.ContentType,
                            ContentType.Application.Json.toString(),
                        ),
                    )
                }

                else -> {
                    error("Unexpected path: $path")
                }
            }
        },
    ) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    @Test
    fun `initSession successfully obtains and validates session token`() = runTest {
        var counter = 1
        val client = createClient { counter++ }
        val authenticator = FakeDeviceAuthenticator()
        val manager = SessionManager(
            challengeClient = PublicAuthSessionChallengeClient(client),
            validateClient = PublicAuthSessionResponseClient(client),
            authenticator = authenticator,
        )

        val token = manager.initSession()

        assertEquals("session-token-2", token)
        assertEquals(1, authenticator.signCallCount)
    }

    @Test
    fun `getToken returns cached token on subsequent calls`() = runTest {
        var counter = 1
        val client = createClient { counter++ }
        val authenticator = FakeDeviceAuthenticator()
        val manager = SessionManager(
            challengeClient = PublicAuthSessionChallengeClient(client),
            validateClient = PublicAuthSessionResponseClient(client),
            authenticator = authenticator,
        )

        val firstToken = manager.getToken()
        val secondToken = manager.getToken()

        assertEquals(firstToken, secondToken)
        assertEquals(1, authenticator.signCallCount)
    }

    @Test
    fun `reset clears session token and forces new session on next getToken`() = runTest {
        var counter = 1
        val client = createClient { counter++ }
        val authenticator = FakeDeviceAuthenticator()
        val manager = SessionManager(
            challengeClient = PublicAuthSessionChallengeClient(client),
            validateClient = PublicAuthSessionResponseClient(client),
            authenticator = authenticator,
        )

        val firstToken = manager.getToken()
        manager.reset()
        val secondToken = manager.getToken()

        assertNotEquals(firstToken, secondToken)
        assertEquals(2, authenticator.signCallCount)
    }
}
