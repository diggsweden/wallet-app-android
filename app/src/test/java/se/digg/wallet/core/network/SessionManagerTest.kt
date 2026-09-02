// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.network

import com.nimbusds.jwt.SignedJWT
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.mockk.coEvery
import io.mockk.mockkObject
import io.mockk.unmockkObject
import java.security.KeyPairGenerator
import java.security.spec.ECGenParameterSpec
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import se.digg.wallet.core.error.AppException
import se.digg.wallet.core.services.KeystoreManager
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

    /**
     * `initSession` reads the device key from the Android Keystore, which does not exist on the
     * JVM, so the tests that drive it substitute a locally generated key pair.
     */
    private suspend fun withStoredAccount(): RecordingHttpClient {
        mockkObject(KeystoreManager)
        coEvery { KeystoreManager.getOrCreateEs256Key(any(), any()) } returns keyPair()
        dao.upsert(User(uuid = null, accountId = "acc-1", credentials = emptyList(), pid = null))
        return RecordingHttpClient {
            respondJson("""{"nonce":"nonce-1","sessionId":"session-1"}""")
        }
    }

    @After
    fun tearDown() = unmockkObject(KeystoreManager)

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
    fun `getToken establishes a session once and then serves it from cache`() = runTest {
        val recorder = withStoredAccount()
        val manager = sessionManager(recorder)

        assertEquals("session-1", manager.getToken())
        assertEquals("session-1", manager.getToken())

        // Establishing a session costs a challenge plus a validation; the second
        // getToken must not repeat them.
        assertEquals(2, recorder.requests.size)
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
    fun `reset forces the next getToken to establish a new session`() = runTest {
        val recorder = withStoredAccount()
        val manager = sessionManager(recorder)
        manager.getToken()

        manager.reset()
        manager.getToken()

        assertEquals(4, recorder.requests.size)
    }
}
