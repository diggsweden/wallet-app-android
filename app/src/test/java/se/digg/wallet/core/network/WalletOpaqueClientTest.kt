// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.network

import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import se.digg.wallet.access_mechanism.api.HSMOperationType
import se.digg.wallet.access_mechanism.model.HSMRequest
import se.digg.wallet.util.RecordingHttpClient
import se.digg.wallet.util.respondJson

class WalletOpaqueClientTest {

    private val publicKey = ECKeyGenerator(Curve.P_256).generate().toECPublicKey()

    private fun hsmRequest() = HSMRequest(outerRequestJws = "outer.request.jws")

    @Test
    fun `registerState sends the thumbprinted device key and maps the response`() = runTest {
        val serverKey = ECKeyGenerator(Curve.P_256).keyID("server-kid").generate()
        val recorder = RecordingHttpClient {
            respondJson(
                """
                {
                  "status": "REGISTERED",
                  "devAuthorizationCode": "dev-code",
                  "opaqueServerId": "server-1",
                  "serverJwsPublicKey": {
                    "kty": "EC", "kid": "server-kid", "crv": "P-256",
                    "x": "${serverKey.x}", "y": "${serverKey.y}"
                  }
                }
                """.trimIndent(),
            )
        }

        val response = WalletOpaqueClient(recorder.client)
            .registerState(publicKey = publicKey, overwrite = false, ttl = "PT10M")

        assertEquals("REGISTERED", response.status)
        assertEquals("dev-code", response.devAuthorizationCode)
        assertEquals("server-1", response.opaqueServerId)
        assertEquals("server-kid", response.serverJwsPublicKey!!.keyID)

        val body = (recorder.requests.single().body as TextContent).text
        assertTrue(body.contains("\"ttl\":\"PT10M\""))
        assertTrue(body.contains("\"kid\":"))
    }

    @Test
    fun `registerState defaults the optional response members`() = runTest {
        val recorder = RecordingHttpClient { respondJson("""{"status":"REGISTERED"}""") }

        val response = WalletOpaqueClient(recorder.client)
            .registerState(publicKey = publicKey, overwrite = true, ttl = null)

        assertEquals("", response.devAuthorizationCode)
        assertEquals("", response.opaqueServerId)
        assertNull(response.serverJwsPublicKey)
    }

    @Test
    fun `perform returns the result of an already complete request`() = runTest {
        val recorder = RecordingHttpClient {
            respondJson("""{"id":"req-1","status":"COMPLETE","result":"hsm-result"}""")
        }

        val result = WalletOpaqueClient(recorder.client)
            .perform(hsmRequest(), HSMOperationType.REGISTER_PIN)

        assertEquals("hsm-result", result)
        assertEquals(1, recorder.requests.size)
        assertTrue(recorder.requests.single().url.toString().contains("REGISTER_PIN"))
    }

    @Test
    fun `every HSM operation maps to a request type the gateway accepts`() = runTest {
        HSMOperationType.entries.forEach { operation ->
            val recorder = RecordingHttpClient {
                respondJson("""{"id":"req","status":"COMPLETE","result":"ok"}""")
            }

            WalletOpaqueClient(recorder.client).perform(hsmRequest(), operation)

            assertTrue(
                "$operation should appear in the request",
                recorder.requests.single().url.toString().contains(operation.name),
            )
        }
    }

    @Test
    fun `perform polls a pending request until it completes`() = runTest {
        var call = 0
        val recorder = RecordingHttpClient {
            call++
            if (call <= 2) {
                respondJson("""{"id":"req-1","status":"PENDING"}""")
            } else {
                respondJson("""{"id":"req-1","status":"COMPLETE","result":"late-result"}""")
            }
        }

        val result = WalletOpaqueClient(recorder.client)
            .perform(hsmRequest(), HSMOperationType.SIGN)

        assertEquals("late-result", result)
        assertEquals(3, recorder.requests.size)
    }

    @Test
    fun `an ERROR status is reported as an IO failure`() = runTest {
        val recorder = RecordingHttpClient { respondJson("""{"id":"req-1","status":"ERROR"}""") }

        val error = runCatching {
            WalletOpaqueClient(recorder.client).perform(hsmRequest(), HSMOperationType.CREATE_KEY)
        }.exceptionOrNull()

        assertTrue(error is IOException)
        assertEquals("HSM operation failed", error!!.message)
    }

    @Test
    fun `a complete response without a result is rejected`() = runTest {
        val recorder = RecordingHttpClient { respondJson("""{"id":"req-1","status":"COMPLETE"}""") }

        val error = runCatching {
            WalletOpaqueClient(recorder.client).perform(hsmRequest(), HSMOperationType.LIST_KEYS)
        }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertEquals("HSM response missing result", error!!.message)
    }

    @Test
    fun `polling gives up after 30 attempts`() = runTest {
        val recorder = RecordingHttpClient { respondJson("""{"id":"req-1","status":"PENDING"}""") }

        val error = runCatching {
            WalletOpaqueClient(recorder.client).perform(hsmRequest(), HSMOperationType.SIGN)
        }.exceptionOrNull()

        assertTrue(error is IOException)
        assertEquals("HSM operation timed out after 30 attempts", error!!.message)
        assertEquals(31, recorder.requests.size)
    }

    @Test
    fun `a gateway failure while registering surfaces to the caller`() = runTest {
        val recorder = RecordingHttpClient {
            respondJson(
                """{"status":500,"title":"Server Error"}""",
                HttpStatusCode.InternalServerError,
            )
        }

        val error = runCatching {
            WalletOpaqueClient(recorder.client)
                .registerState(publicKey = publicKey, overwrite = false, ttl = null)
        }.exceptionOrNull()

        assertTrue(error != null)
    }
}
