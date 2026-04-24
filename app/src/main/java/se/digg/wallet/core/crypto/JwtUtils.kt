// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.crypto

import com.nimbusds.jose.EncryptionMethod
import com.nimbusds.jose.JWEAlgorithm
import com.nimbusds.jose.JWEHeader
import com.nimbusds.jose.JWEObject
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.Payload
import com.nimbusds.jose.crypto.ECDHDecrypter
import com.nimbusds.jose.crypto.ECDHEncrypter
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import java.security.KeyPair
import java.time.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import se.digg.wallet.core.extensions.toECKey

object JwtUtils {
    inline fun <reified T> encryptJwe(
        payload: T,
        recipientKey: JWK,
        encryptionMethod: EncryptionMethod,
        algorithm: JWEAlgorithm = JWEAlgorithm.ECDH_ES,
    ): String {
        val jweHeader = JWEHeader.Builder(algorithm, encryptionMethod).build()
        val json = Json.encodeToString(payload)
        val jweBody = JWEObject(jweHeader, Payload(json))
        val encrypter = ECDHEncrypter(recipientKey.toECKey())
        jweBody.encrypt(encrypter)
        return jweBody.serialize()
    }

    inline fun <reified T> decryptJwe(compactString: String, decryptionKeyPair: KeyPair): T {
        val jwe = JWEObject.parse(compactString)
        val decrypter =
            ECDHDecrypter(decryptionKeyPair.private, null, Curve.P_256)
        jwe.decrypt(decrypter)
        val serializer = Json { ignoreUnknownKeys = true }
        val jsonString = jwe.payload.toBytes().decodeToString()
        return serializer.decodeFromString<T>(jsonString)
    }

    inline fun <reified T> signJWT(
        keyPair: KeyPair,
        payload: T,
        headers: Map<String, Any>,
        includeJwk: Boolean = false,
    ): SignedJWT {
        val now = Instant.now().epochSecond.toInt()

        val defaultJwtClaims = DefaultJwtClaims(
            iat = now,
            nbf = now,
            exp = now + 600,
        )

        val claimsSerializer = jwtClaimsSerializer(serializer<T>())
        val encoded = Json.encodeToString(
            claimsSerializer,
            JwtClaims(defaults = defaultJwtClaims, payload = payload),
        )
        val algorithm = JWSAlgorithm.ES256

        val header = JWSHeader.Builder(algorithm)
            .customParams(headers)
            .apply {
                if (includeJwk) {
                    jwk(keyPair.toECKey())
                }
            }
            .build()

        val claimsSet = JWTClaimsSet.parse(encoded)
        val signedJwt = SignedJWT(header, claimsSet)
        signedJwt.sign(WalletSigner(keyPair))

        return signedJwt
    }
}
