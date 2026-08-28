// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.services

import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import se.digg.wallet.core.network.RequestAuthorization
import se.digg.wallet.data.CredentialRequestModel
import se.digg.wallet.data.Proof
import se.digg.wallet.util.RecordingHttpClient
import se.digg.wallet.util.respondJson
import se.digg.wallet.util.respondText

private const val CREDENTIAL_URL = "https://issuer.example.test/credential"
private const val RESPONSE_URL = "https://verifier.example.test/response"

class OpenIdNetworkServiceTest {

    private val bearer = RequestAuthorization.Bearer("token-1")

    private fun bodyOf(request: HttpRequestData): String =
        (request.body as? TextContent)?.text ?: ""

    @Test
    fun `fetchCredential posts the request and parses the credential list`() = runTest {
        val recorder = RecordingHttpClient {
            respondJson("""{"credentials":[{"credential":"sd-jwt-1"}]}""")
        }

        val response = OpenIdNetworkService(recorder.client).fetchCredential(
            url = CREDENTIAL_URL,
            authorization = bearer,
            request = CredentialRequestModel(
                credentialConfigurationId = "pid",
                proofs = Proof(jwt = listOf("proof-jwt")),
            ),
        )

        assertEquals(listOf("sd-jwt-1"), response.credentials.map { it.credential })
        val request = recorder.requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("Bearer token-1", request.headers[HttpHeaders.Authorization])
        assertTrue(bodyOf(request).contains("\"credential_configuration_id\":\"pid\""))
    }

    @Test
    fun `the JWE credential overload returns the raw body`() = runTest {
        val recorder = RecordingHttpClient { respondText("jwe.compact.serialization") }

        val body = OpenIdNetworkService(recorder.client).fetchCredential(
            url = CREDENTIAL_URL,
            authorization = bearer,
            jweBody = "encrypted-request",
        )

        assertEquals("jwe.compact.serialization", body)
        assertEquals("encrypted-request", bodyOf(recorder.requests.single()))
        assertEquals("application/jwt", recorder.requests.single().headers[HttpHeaders.Accept])
    }

    @Test
    fun `the JWE credential overload reports the status and body on failure`() = runTest {
        val recorder = RecordingHttpClient {
            respondText("issuer rejected the proof", HttpStatusCode.BadRequest)
        }

        val error = runCatching {
            OpenIdNetworkService(recorder.client).fetchCredential(
                url = CREDENTIAL_URL,
                authorization = bearer,
                jweBody = "encrypted-request",
            )
        }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertTrue(error!!.message!!.contains("400"))
        assertTrue(error.message!!.contains("issuer rejected the proof"))
    }

    @Test
    fun `fetchNonce parses the c_nonce response`() = runTest {
        val recorder = RecordingHttpClient { respondJson("""{"c_nonce":"nonce-1"}""") }

        val nonce = OpenIdNetworkService(recorder.client)
            .fetchNonce("https://issuer.example.test/nonce")

        assertEquals("nonce-1", nonce.nonce)
        assertEquals(HttpMethod.Post, recorder.requests.single().method)
    }

    @Test
    fun `postVpToken reports a redirect when the verifier returns one`() = runTest {
        val recorder = RecordingHttpClient {
            respondJson("""{"redirect_uri":"https://verifier.example.test/done"}""")
        }

        val result = OpenIdNetworkService(recorder.client)
            .postVpToken(url = RESPONSE_URL, body = "vp_token=abc&state=xyz")

        assertEquals(PresentationResult.Redirect("https://verifier.example.test/done"), result)
        assertEquals("vp_token=abc&state=xyz", bodyOf(recorder.requests.single()))
        assertEquals(
            "application/x-www-form-urlencoded",
            recorder.requests.single().body.contentType.toString(),
        )
    }

    @Test
    fun `postVpToken reports plain success when there is no redirect`() = runTest {
        val recorder = RecordingHttpClient { respondJson("{}") }

        val result = OpenIdNetworkService(recorder.client)
            .postVpToken(url = RESPONSE_URL, body = "vp_token=abc")

        assertEquals(PresentationResult.Success, result)
    }

    @Test
    fun `postVpToken reports the status on failure`() = runTest {
        val recorder = RecordingHttpClient {
            respondJson("""{"error":"invalid_request"}""", HttpStatusCode.BadRequest)
        }

        val error = runCatching {
            OpenIdNetworkService(recorder.client)
                .postVpToken(url = RESPONSE_URL, body = "vp_token=abc")
        }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertTrue(error!!.message!!.contains("400"))
    }
}
