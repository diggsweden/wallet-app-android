// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.data.model

import com.nimbusds.jose.JWEAlgorithm
import io.ktor.http.formUrlEncode
import io.ktor.http.parameters
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import se.digg.wallet.core.crypto.CryptoSpec
import se.digg.wallet.core.crypto.JwtUtils

@Serializable
data class VpTokenResponse(
    @SerialName("vp_token")
    val vpToken: Map<String, List<String>>,
    val nonce: String,
    val state: String? = null,
) {
    fun toDirectPost(): String = parameters {
        append("vp_token", Json.encodeToString(vpToken))
        append("nonce", nonce)
        state?.let { value ->
            append("state", value)
        }
    }.formUrlEncode()

    fun toDirectPostJwt(cryptoSpec: CryptoSpec): String {
        val jwe = JwtUtils.encryptJwe(
            payload = this,
            recipientKey = cryptoSpec.jwk,
            encryptionMethod = cryptoSpec.encryptionMethod,
            algorithm = cryptoSpec.encryptionAlgorithm ?: JWEAlgorithm.ECDH_ES,
        )

        return parameters {
            append("response", jwe)
        }.formUrlEncode()
    }
}
