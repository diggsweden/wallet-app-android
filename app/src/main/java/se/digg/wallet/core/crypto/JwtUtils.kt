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
import com.nimbusds.jose.util.Base64URL
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import java.security.KeyPair
import java.time.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import se.digg.wallet.core.extensions.toECKey

object JwtUtils {
    @PublishedApi
    internal inline fun <reified T> encodeClaims(payload: T): String {
        val now = Instant.now().epochSecond.toInt()
        return Json.encodeToString(
            JwtClaimsSerializer(serializer<T>()),
            JwtClaims(
                defaults = DefaultJwtClaims(iat = now, nbf = now, exp = now + 600),
                payload = payload,
            ),
        )
    }

    /**
     * Signs an ES256 JWT with a locally held [keyPair] (e.g. an Android Keystore key).
     * Set [includeJwk] to embed the public key in the protected header.
     */
    inline fun <reified T> signJwt(
        keyPair: KeyPair,
        payload: T,
        headers: Map<String, Any>,
        includeJwk: Boolean = false,
    ): SignedJWT {
        val encoded = encodeClaims(payload)

        val header = JWSHeader.Builder(JWSAlgorithm.ES256)
            .customParams(headers)
            .apply {
                if (includeJwk) {
                    jwk(keyPair.toECKey())
                }
            }
            .build()

        val signedJwt = SignedJWT(header, JWTClaimsSet.parse(encoded))
        signedJwt.sign(WalletSigner(keyPair))
        return signedJwt
    }

    /**
     * Signs an ES256 JWT with an external signer (e.g. an HSM key): builds the
     * `header.payload` signing input and delegates the signature to [sign], which
     * must return the base64url-encoded signature. [jwk], when given, is embedded
     * in the protected header.
     */
    suspend inline fun <reified T> signJwtWith(
        payload: T,
        headers: Map<String, Any>,
        jwk: JWK? = null,
        sign: suspend (ByteArray) -> String,
    ): String {
        val payloadBytes = encodeClaims(payload).toByteArray(Charsets.UTF_8)

        val header = JWSHeader.Builder(JWSAlgorithm.ES256)
            .customParams(headers)
            .apply {
                if (jwk != null) {
                    jwk(jwk)
                }
            }
            .build()

        val signingInput = "${header.toBase64URL()}.${Base64URL.encode(payloadBytes)}"
        val signature = sign(signingInput.toByteArray(Charsets.US_ASCII))
        return "$signingInput.$signature"
    }

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
        val decrypter = ECDHDecrypter(decryptionKeyPair.private, null, Curve.P_256)
        jwe.decrypt(decrypter)
        val serializer = Json { ignoreUnknownKeys = true }
        val jsonString = jwe.payload.toBytes().decodeToString()
        return serializer.decodeFromString<T>(jsonString)
    }
}
