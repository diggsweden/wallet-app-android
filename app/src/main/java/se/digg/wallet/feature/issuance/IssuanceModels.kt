// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

@file:Suppress("ktlint:standard:filename")

package se.digg.wallet.feature.issuance

import com.nimbusds.jose.jwk.JWK
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import se.digg.wallet.core.crypto.JwtHeader

@Serializable
data class IssuanceProofPayload(val aud: String, val nonce: String?, val iss: String)

@Serializable
data class ProofJwtHeader(
    override val typ: String = "openid4vci-proof+jwt",
    override val kid: String? = null,
    override val jwk: JsonObject? = null,
    @SerialName("key_attestation")
    val keyAttestation: String? = null,
) : JwtHeader {
    companion object {
        fun withJwk(key: JWK) = ProofJwtHeader(
            jwk = Json.parseToJsonElement(key.toPublicJWK().toJSONString()).jsonObject,
        )

        fun withKeyAttestation(keyAttestation: String) = ProofJwtHeader(
            kid = "0",
            keyAttestation = keyAttestation,
        )
    }
}
