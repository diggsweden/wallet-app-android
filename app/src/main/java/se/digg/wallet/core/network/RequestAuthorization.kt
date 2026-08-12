// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.network

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.Url

const val DPOP_HEADER = "DPoP"

sealed interface RequestAuthorization {
    data class Bearer(val accessToken: String) : RequestAuthorization

    data class Dpop(val accessToken: String, val proofProvider: DpopProofProvider) :
        RequestAuthorization

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
