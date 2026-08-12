// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.network

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val NONCE_ERROR_CODE = "use_dpop_nonce"
private const val DPOP_NONCE_HEADER = "DPoP-Nonce"

private val clientErrorStatuses = 400 until 500

private val errorJson = Json { ignoreUnknownKeys = true }

/** An OAuth error response (RFC 6749 §5.2). */
@Serializable
private data class OAuthErrorResponse(val error: String)

/**
 * The nonce to repeat this request with, if the response is a `use_dpop_nonce`
 * challenge (RFC 9449 §8).
 *
 * Reads the response body, so this must only be called on a saved call — Ktor
 * bodies are single-consumption and the caller still needs to read it when the
 * response turns out not to be a challenge.
 */
suspend fun HttpResponse.dpopNonceChallenge(): String? {
    if (status.value !in clientErrorStatuses) {
        return null
    }

    return dpopNonceChallenge(
        status = status,
        headers = headers,
        body = runCatching { bodyAsText() }.getOrNull(),
    )
}

/**
 * Servers challenge in one of two ways in practice: a resource server over
 * `WWW-Authenticate`, an authorization server over the error body. Both are
 * accepted, but only on a 4xx — a 5xx that happens to name the code is a fault,
 * not a challenge.
 */
internal fun dpopNonceChallenge(status: HttpStatusCode, headers: Headers, body: String?): String? {
    val challenged = status.value in clientErrorStatuses &&
        (headers.isNonceChallenge() || isNonceChallenge(body))

    if (!challenged) {
        return null
    }

    return headers[DPOP_NONCE_HEADER]?.takeIf { it.isNotBlank() }
}

private fun Headers.isNonceChallenge(): Boolean {
    val challenge = get(HttpHeaders.WWWAuthenticate) ?: return false

    return challenge.contains(DPOP_HEADER, ignoreCase = true) &&
        challenge.contains("error=\"$NONCE_ERROR_CODE\"", ignoreCase = true)
}

private fun isNonceChallenge(body: String?): Boolean {
    if (body.isNullOrBlank()) {
        return false
    }

    val error = runCatching {
        errorJson.decodeFromString<OAuthErrorResponse>(body)
    }.getOrNull()

    return error?.error == NONCE_ERROR_CODE
}
