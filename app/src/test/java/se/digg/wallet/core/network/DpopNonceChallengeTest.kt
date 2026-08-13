// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.network

import io.ktor.http.Headers
import io.ktor.http.HttpStatusCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DpopNonceChallengeTest {

    private fun headers(vararg entries: Pair<String, String>): Headers = Headers.build {
        entries.forEach { (name, value) ->
            append(name, value)
        }
    }

    @Test
    fun `resource server challenges over WWW-Authenticate with no error body`() {
        val nonce = dpopNonceChallenge(
            status = HttpStatusCode.Unauthorized,
            headers = headers(
                "WWW-Authenticate" to
                    """DPoP error="use_dpop_nonce", error_description="nonce needed"""",
                "DPoP-Nonce" to "nonce-abc",
            ),
            body = null,
        )

        assertEquals("nonce-abc", nonce)
    }

    @Test
    fun `authorization server challenges over the error body`() {
        val nonce = dpopNonceChallenge(
            status = HttpStatusCode.BadRequest,
            headers = headers("DPoP-Nonce" to "nonce-abc"),
            body = """{"error":"use_dpop_nonce"}""",
        )

        assertEquals("nonce-abc", nonce)
    }

    @Test
    fun `challenge is recognised across the 4xx range`() {
        val nonce = dpopNonceChallenge(
            status = HttpStatusCode.Forbidden,
            headers = headers("DPoP-Nonce" to "nonce-abc"),
            body = """{"error":"use_dpop_nonce","error_description":"nonce needed"}""",
        )

        assertEquals("nonce-abc", nonce)
    }

    @Test
    fun `header names are matched case-insensitively`() {
        val nonce = dpopNonceChallenge(
            status = HttpStatusCode.Unauthorized,
            headers = headers(
                "www-authenticate" to """dpop error="use_dpop_nonce"""",
                "dpop-nonce" to "nonce-abc",
            ),
            body = null,
        )

        assertEquals("nonce-abc", nonce)
    }

    @Test
    fun `a different error code is a refusal, not a challenge`() {
        val nonce = dpopNonceChallenge(
            status = HttpStatusCode.BadRequest,
            headers = headers("DPoP-Nonce" to "nonce-abc"),
            body = """{"error":"invalid_dpop_proof"}""",
        )

        assertNull(nonce)
    }

    @Test
    fun `a WWW-Authenticate naming another error is not a challenge`() {
        val nonce = dpopNonceChallenge(
            status = HttpStatusCode.Unauthorized,
            headers = headers(
                "WWW-Authenticate" to """DPoP error="invalid_token"""",
                "DPoP-Nonce" to "nonce-abc",
            ),
            body = null,
        )

        assertNull(nonce)
    }

    @Test
    fun `a server fault is not a challenge, even when it names the error code`() {
        val nonce = dpopNonceChallenge(
            status = HttpStatusCode.ServiceUnavailable,
            headers = headers("DPoP-Nonce" to "nonce-abc"),
            body = """{"error":"use_dpop_nonce"}""",
        )

        assertNull(nonce)
    }

    @Test
    fun `a challenge without a nonce to use is not actionable`() {
        val nonce = dpopNonceChallenge(
            status = HttpStatusCode.Unauthorized,
            headers = headers("WWW-Authenticate" to """DPoP error="use_dpop_nonce""""),
            body = null,
        )

        assertNull(nonce)
    }

    @Test
    fun `a body that is not an error response is not a challenge`() {
        val nonce = dpopNonceChallenge(
            status = HttpStatusCode.BadRequest,
            headers = headers("DPoP-Nonce" to "nonce-abc"),
            body = "not json at all",
        )

        assertNull(nonce)
    }

    @Test
    fun `a successful response is never a challenge`() {
        val nonce = dpopNonceChallenge(
            status = HttpStatusCode.OK,
            headers = headers("DPoP-Nonce" to "nonce-abc"),
            body = """{"error":"use_dpop_nonce"}""",
        )

        assertNull(nonce)
    }
}
