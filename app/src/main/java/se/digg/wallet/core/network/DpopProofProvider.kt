// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.network

import io.ktor.http.HttpMethod
import io.ktor.http.Url

interface DpopProofProvider {
    /**
     * @param accessToken Bound as the `ath` claim; null for requests that have no
     *   access token yet, such as PAR.
     * @param nonce Server-supplied nonce from a `use_dpop_nonce` challenge.
     */
    suspend fun proof(
        endpoint: Url,
        method: HttpMethod,
        accessToken: String?,
        nonce: String?,
    ): String
}
