// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.network

import com.nimbusds.jwt.SignedJWT
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import se.digg.wallet.core.crypto.DpopProofBuilder

private const val CREDENTIAL_ENDPOINT = "https://issuer.example.com/credential?format=vc"

class DpopPluginTest {

    private val proofBuilder = DpopProofBuilder()
    private val requests = mutableListOf<HttpRequestData>()

    private fun client(
        handle: MockRequestHandleScope.(attempt: Int) -> HttpResponseData,
    ): HttpClient {
        val engine = MockEngine { request ->
            requests += request
            handle(requests.size)
        }

        return HttpClient(engine) {
            install(dpopPlugin)
        }
    }

    private fun MockRequestHandleScope.nonceChallenge(
        nonce: String? = "nonce-abc",
    ): HttpResponseData = respond(
        content = """{"error":"use_dpop_nonce"}""",
        status = HttpStatusCode.BadRequest,
        headers = Headers.build {
            append(HttpHeaders.ContentType, "application/json")
            if (nonce != null) {
                append("DPoP-Nonce", nonce)
            }
        },
    )

    private fun proofOf(request: HttpRequestData): SignedJWT =
        SignedJWT.parse(requireNotNull(request.headers[DPOP_HEADER]))

    @Test
    fun `a dpop token is sent with the DPoP scheme and a proof bound to the request`() = runTest {
        val client = client {
            respond("ok")
        }

        client.post(CREDENTIAL_ENDPOINT) {
            authorizeWith(RequestAuthorization.Dpop("access-token", proofBuilder))
        }

        assertEquals(1, requests.size)
        assertEquals("DPoP access-token", requests[0].headers[HttpHeaders.Authorization])

        val claims = proofOf(requests[0]).jwtClaimsSet
        assertEquals("POST", claims.getStringClaim("htm"))
        assertEquals("https://issuer.example.com/credential", claims.getStringClaim("htu"))
        assertNull(claims.getStringClaim("nonce"))
    }

    @Test
    fun `a nonce challenge is retried once with a fresh proof carrying the nonce`() = runTest {
        val client = client { attempt ->
            if (attempt == 1) {
                nonceChallenge()
            } else {
                respond("credential")
            }
        }

        val response = client.post(CREDENTIAL_ENDPOINT) {
            authorizeWith(RequestAuthorization.Dpop("access-token", proofBuilder))
        }

        assertEquals(2, requests.size)
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("credential", response.bodyAsText())

        val first = proofOf(requests[0]).jwtClaimsSet
        val second = proofOf(requests[1]).jwtClaimsSet
        assertEquals("nonce-abc", second.getStringClaim("nonce"))
        assertNotEquals(first.jwtid, second.jwtid)
        assertEquals(first.getStringClaim("ath"), second.getStringClaim("ath"))
    }

    @Test
    fun `a retry that is challenged again is not retried a second time`() = runTest {
        val client = client {
            nonceChallenge(nonce = "nonce-rotated")
        }

        val response = client.post(CREDENTIAL_ENDPOINT) {
            authorizeWith(RequestAuthorization.Dpop("access-token", proofBuilder))
        }

        assertEquals(2, requests.size)
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `a bearer request carries no proof and is never retried`() = runTest {
        val client = client {
            nonceChallenge()
        }

        val response = client.post(CREDENTIAL_ENDPOINT) {
            authorizeWith(RequestAuthorization.Bearer("access-token"))
        }

        assertEquals(1, requests.size)
        assertEquals("Bearer access-token", requests[0].headers[HttpHeaders.Authorization])
        assertNull(requests[0].headers[DPOP_HEADER])
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `a 4xx that is not a challenge is returned with its body still readable`() = runTest {
        val client = client {
            respond(
                content = """{"error":"invalid_dpop_proof"}""",
                status = HttpStatusCode.BadRequest,
                headers = Headers.build {
                    append(HttpHeaders.ContentType, "application/json")
                    append("DPoP-Nonce", "nonce-abc")
                },
            )
        }

        val response = client.post(CREDENTIAL_ENDPOINT) {
            authorizeWith(RequestAuthorization.Dpop("access-token", proofBuilder))
        }

        assertEquals(1, requests.size)
        assertEquals("""{"error":"invalid_dpop_proof"}""", response.bodyAsText())
    }

    @Test
    fun `an unauthorized request is passed through untouched`() = runTest {
        val client = client {
            respond("ok")
        }

        client.post(CREDENTIAL_ENDPOINT)

        assertEquals(1, requests.size)
        assertNull(requests[0].headers[HttpHeaders.Authorization])
        assertNull(requests[0].headers[DPOP_HEADER])
    }
}
