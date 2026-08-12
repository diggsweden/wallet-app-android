// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.network

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.Url

/** The `DPoP` request header carrying the proof JWT (RFC 9449 §4). */
const val DPOP_HEADER = "DPoP"

/**
 * How a request proves it may use its access token. The scheme follows the token
 * type the authorization server issued, so it is decided per token, not per server.
 */
sealed interface RequestAuthorization {
    data class Bearer(val accessToken: String) : RequestAuthorization

    /**
     * @property proofProvider Holds the key the token was issued against. A
     *   provider — rather than a proof string — is carried here because a proof is
     *   only valid for one attempt against one endpoint.
     */
    data class Dpop(val accessToken: String, val proofProvider: DpopProofProvider) :
        RequestAuthorization

    /**
     * The authorization headers for one attempt. A DPoP proof is bound to the
     * endpoint, the method and the server's nonce (RFC 9449 §4.2), so it is built
     * here, at send time, rather than prepared by the caller.
     */
    suspend fun headers(endpoint: Url, method: HttpMethod, nonce: String?): Map<String, String> =
        when (this) {
            is Bearer -> mapOf(HttpHeaders.Authorization to "Bearer $accessToken")

            is Dpop -> mapOf(
                HttpHeaders.Authorization to "$DPOP_HEADER $accessToken",
                DPOP_HEADER to proofProvider.proof(
                    endpoint = endpoint,
                    method = method,
                    accessToken = accessToken,
                    nonce = nonce,
                ),
            )
        }
}
