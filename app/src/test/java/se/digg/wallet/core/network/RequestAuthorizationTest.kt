// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.network

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.Url
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

private class RecordingProofProvider : DpopProofProvider {
    var lastAccessToken: String? = null
    var lastNonce: String? = null
    var lastMethod: HttpMethod? = null

    override suspend fun proof(
        endpoint: Url,
        method: HttpMethod,
        accessToken: String?,
        nonce: String?,
    ): String {
        lastAccessToken = accessToken
        lastNonce = nonce
        lastMethod = method
        return "dpop-proof"
    }
}

class RequestAuthorizationTest {

    private val endpoint = Url("https://issuer.example.test/credential")

    @Test
    fun `a bearer authorization contributes only the Authorization header`() = runTest {
        val headers = RequestAuthorization.Bearer("token-1")
            .headers(endpoint, HttpMethod.Post, nonce = "ignored")

        assertEquals(mapOf(HttpHeaders.Authorization to "Bearer token-1"), headers)
    }

    @Test
    fun `a DPoP authorization binds the access token into a fresh proof`() = runTest {
        val proofProvider = RecordingProofProvider()

        val headers = RequestAuthorization.Dpop("token-1", proofProvider)
            .headers(endpoint, HttpMethod.Post, nonce = "nonce-1")

        assertEquals("DPoP token-1", headers[HttpHeaders.Authorization])
        assertEquals("dpop-proof", headers[DPOP_HEADER])
        assertEquals("token-1", proofProvider.lastAccessToken)
        assertEquals("nonce-1", proofProvider.lastNonce)
        assertEquals(HttpMethod.Post, proofProvider.lastMethod)
    }

    @Test
    fun `a DPoP proof is requested without a nonce until the server challenges`() = runTest {
        val proofProvider = RecordingProofProvider()

        RequestAuthorization.Dpop("token-1", proofProvider)
            .headers(endpoint, HttpMethod.Get, nonce = null)

        assertEquals(null, proofProvider.lastNonce)
        assertEquals(HttpMethod.Get, proofProvider.lastMethod)
    }
}
