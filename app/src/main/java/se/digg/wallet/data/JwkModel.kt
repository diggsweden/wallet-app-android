// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.data

import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.jwk.OctetSequenceKey
import com.nimbusds.jose.jwk.RSAKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JwkModel(
    val kty: String,
    val use: String? = null,
    @SerialName("key_ops")
    val keyOps: List<String>? = null,
    val alg: String? = null,
    val kid: String? = null,
    @SerialName("x5u")
    val x5u: String? = null,
    @SerialName("x5c")
    val x5c: List<String>? = null,
    @SerialName("x5t")
    val x5t: String? = null,
    @SerialName("x5t#S256")
    val x5tS256: String? = null,
    val crv: String? = null,
    val x: String? = null,
    val y: String? = null,
    val n: String? = null,
    val e: String? = null,
    val k: String? = null,
)

fun JWK.toJwkModel(): JwkModel = when (this) {
    is ECKey -> JwkModel(
        kty = "EC",
        crv = curve.name,
        x = x.toString(),
        y = y.toString(),
        use = keyUse?.identifier(),
        keyOps = keyOperations?.map { it.identifier() },
        alg = algorithm?.name,
        kid = keyID,
    )

    is RSAKey -> JwkModel(
        kty = "RSA",
        n = modulus.toString(),
        e = publicExponent.toString(),
        use = keyUse?.identifier(),
        keyOps = keyOperations?.map { it.identifier() },
        alg = algorithm?.name,
        kid = keyID,
    )

    is OctetSequenceKey -> JwkModel(
        kty = "oct",
        k = keyValue.toString(),
        use = keyUse?.identifier(),
        keyOps = keyOperations?.map { it.identifier() },
        alg = algorithm?.name,
        kid = keyID,
    )

    else -> error("Unsupported JWK type: ${this::class.simpleName}")
}
