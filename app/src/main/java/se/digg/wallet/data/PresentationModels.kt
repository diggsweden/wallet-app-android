// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

@file:Suppress("ktlint:standard:filename")

package se.digg.wallet.data

import com.nimbusds.jose.JWEAlgorithm
import eu.europa.ec.eudi.sdjwt.JwtAndClaims
import eu.europa.ec.eudi.sdjwt.SdJwt
import eu.europa.ec.eudi.sdjwt.vc.ClaimPath
import io.ktor.http.formUrlEncode
import io.ktor.http.parameters
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import se.digg.wallet.core.crypto.CryptoSpec
import se.digg.wallet.core.crypto.JwtUtils

@Serializable
data class KeybindingPayload(
    val aud: String,
    val nonce: String,
    @SerialName("sd_hash")
    val sdHash: String,
)

data class CredentialQuery(
    val id: String,
    val required: Boolean = true,
    val vctValues: List<String>,
    val claimPaths: Set<ClaimPath>,
)

data class PresentationItem(
    val id: String,
    val isChecked: Boolean,
    val isRequired: Boolean,
    val claims: List<ClaimUiModel>,
    val disclosedSdJwt: SdJwt<JwtAndClaims>,
)

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
