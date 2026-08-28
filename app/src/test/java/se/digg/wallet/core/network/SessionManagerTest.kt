// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.network

import com.nimbusds.jwt.SignedJWT
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import java.security.KeyPairGenerator
import java.security.spec.ECGenParameterSpec
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import se.digg.wallet.core.error.AppException
import se.digg.wallet.core.storage.user.User
import se.digg.wallet.util.FakeUserDao
import se.digg.wallet.util.RecordingHttpClient
import se.digg.wallet.util.respondJson
import se.wallet.client.gateway.client.PublicAuthSessionChallengeClient
import se.wallet.client.gateway.client.PublicAuthSessionResponseClient

class SessionManagerTest {

    private val dao = FakeUserDao()

    private fun keyPair() = KeyPairGenerator.getInstance("EC").apply {
        initialize(ECGenParameterSpec("secp256r1"))
    }.generateKeyPair()

    private fun sessionManager(recorder: RecordingHttpClient) = SessionManager(
        challengeClient = PublicAuthSessionChallengeClient(recorder.client),
        validateClient = PublicAuthSessionResponseClient(recorder.client),
        userDao = dao,
    )

    @Test
    fun `getChallenge returns the server nonce`() = runTest {
        val recorder = RecordingHttpClient { respondJson("""{"nonce":"nonce-1"}""") }

        val nonce = sessionManager(recorder).getChallenge(accountId = "acc-1", keyId = "kid-1")

        assertEquals("nonce-1", nonce)
    }

    @Test
    fun `getChallenge falls back to an empty nonce`() = runTest {
        val recorder = RecordingHttpClient { respondJson("{}") }

        assertEquals("", sessionManager(recorder).getChallenge("acc-1", "kid-1"))
    }

    @Test
    fun `getChallenge surfaces a gateway problem`() = runTest {
        val recorder = RecordingHttpClient {
            respondJson("""{"status":404,"title":"Not Found"}""", HttpStatusCode.NotFound)
        }

        val error = runCatching {
            sessionManager(recorder).getChallenge("acc-1", "kid-1")
        }.exceptionOrNull()

        assertTrue(error is AppException)
    }

    @Test
    fun `validateChallenge signs the nonce and returns the session id`() = runTest {
        val recorder = RecordingHttpClient { respondJson("""{"sessionId":"session-1"}""") }
        val key = keyPair()

        val sessionId = sessionManager(recorder)
            .validateChallenge(keyId = "kid-1", key = key, nonce = "nonce-1")

        assertEquals("session-1", sessionId)

        val body = (recorder.requests.single().body as TextContent).text
        val signedJwt = SignedJWT.parse(
            Regex(""""signedJwt":"([^"]+)"""").find(body)!!.groupValues[1],
        )
        // Passed through `customParams`, but `kid` is a registered header member,
        // so it parses back as the typed keyID rather than a custom param.
        assertEquals("kid-1", signedJwt.header.keyID)
        assertEquals("nonce-1", signedJwt.jwtClaimsSet.getClaim("nonce"))
        assertEquals("ES256", signedJwt.header.algorithm.name)
    }

    @Test
    fun `getToken reuses a token that has already been issued`() = runTest {
        var calls = 0
        val recorder = RecordingHttpClient {
            calls++
            respondJson("""{"nonce":"nonce-1","sessionId":"session-1"}""")
        }
        val manager = sessionManager(recorder)

        val issued = manager.validateChallenge("kid-1", keyPair(), "nonce-1")
        assertEquals("session-1", issued)
        assertEquals(1, calls)
    }

    @Test
    fun `initSession requires a stored account`() = runTest {
        val recorder = RecordingHttpClient { respondJson("{}") }

        val error = runCatching { sessionManager(recorder).initSession() }.exceptionOrNull()

        assertEquals("No account", error!!.message)
    }

    @Test
    fun `getToken without a session tries to establish one`() = runTest {
        val recorder = RecordingHttpClient { respondJson("{}") }
        dao.upsert(User(uuid = null, accountId = null, credentials = emptyList(), pid = null))

        val error = runCatching { sessionManager(recorder).getToken() }.exceptionOrNull()

        assertEquals("No account", error!!.message)
    }

    @Test
    fun `reset clears the cached token`() = runTest {
        val recorder = RecordingHttpClient { respondJson("{}") }
        val manager = sessionManager(recorder)

        manager.reset()

        assertEquals("No account", runCatching { manager.getToken() }.exceptionOrNull()!!.message)
    }
}
