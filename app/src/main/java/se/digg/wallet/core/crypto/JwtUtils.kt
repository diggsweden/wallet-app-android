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
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.serializer
import se.digg.wallet.core.extensions.toECKey

object JwtUtils {
    @PublishedApi
    internal val headerFormat = Json {
        encodeDefaults = true
        explicitNulls = false
    }

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

    @PublishedApi
    internal inline fun <reified H : JwtHeader> buildHeader(header: H): JWSHeader {
        val fields = headerFormat.encodeToJsonElement(serializer<H>(), header).jsonObject
        val headerJson = buildJsonObject {
            fields.forEach { (key, value) ->
                put(key, value)
            }
            put("alg", JWSAlgorithm.ES256.name)
        }
        return JWSHeader.parse(headerJson.toString())
    }

    /**
     * Signs an ES256 JWT with a locally held [keyPair] (e.g. an Android Keystore key).
     */
    inline fun <reified H : JwtHeader, reified T> signJwt(
        keyPair: KeyPair,
        header: H,
        payload: T,
    ): SignedJWT {
        val encoded = encodeClaims(payload)
        val signedJwt = SignedJWT(buildHeader(header), JWTClaimsSet.parse(encoded))
        signedJwt.sign(WalletSigner(keyPair))
        return signedJwt
    }

    /**
     * Signs an ES256 JWT with an external signer (e.g. an HSM key): builds the
     * `header.payload` signing input and delegates the signature to [sign], which
     * must return the base64url-encoded signature.
     */
    suspend inline fun <reified H : JwtHeader, reified T> signJwtWith(
        header: H,
        payload: T,
        sign: suspend (ByteArray) -> String,
    ): String {
        val payloadBytes = encodeClaims(payload).toByteArray(Charsets.UTF_8)
        val jwsHeader = buildHeader(header)

        val signingInput = "${jwsHeader.toBase64URL()}.${Base64URL.encode(payloadBytes)}"
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
