// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.network

import io.ktor.http.HttpMethod
import io.ktor.http.Url

/**
 * Builds DPoP proofs (RFC 9449 §4.2) for the requests the wallet sends itself.
 *
 * A proof is bound to the request it accompanies, so it cannot be built ahead of
 * time and handed to the networking layer as a string — the layer that sends the
 * request has to be able to build one, once per attempt.
 */
interface DpopProofProvider {
    /**
     * @param endpoint Bound into the proof as the `htu` claim, stripped of query
     *   and fragment.
     * @param method Bound into the proof as the `htm` claim.
     * @param accessToken Bound into the proof as the `ath` claim. Omitted when
     *   proving a request that has no access token yet, such as PAR.
     * @param nonce Server-supplied nonce from a `use_dpop_nonce` challenge.
     */
    suspend fun proof(
        endpoint: Url,
        method: HttpMethod,
        accessToken: String?,
        nonce: String?,
    ): String
}
