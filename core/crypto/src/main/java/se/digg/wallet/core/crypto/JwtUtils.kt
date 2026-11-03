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
import com.nimbusds.jose.JWSObject
import com.nimbusds.jose.Payload
import com.nimbusds.jose.crypto.ECDHDecrypter
import com.nimbusds.jose.crypto.ECDHEncrypter
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.util.Base64URL
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import java.security.KeyPair
import java.security.PublicKey
import java.security.cert.X509Certificate
import java.time.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.serializer

object JwtUtils {
    @PublishedApi
    internal val headerFormat = Json {
        encodeDefaults = true
        explicitNulls = false
    }

    @PublishedApi
    internal val payloadFormat = Json { ignoreUnknownKeys = true }

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
        val jweHeader =
            JWEHeader.Builder(algorithm, encryptionMethod).keyID(recipientKey.keyID)
                .build()
        val json = Json.encodeToString(payload)
        val jweBody = JWEObject(jweHeader, Payload(json))
        val encrypter = ECDHEncrypter(recipientKey.toECKey())
        jweBody.encrypt(encrypter)
        return jweBody.serialize()
    }

    inline fun <reified T> verifyJws(
        jwsString: String,
        validateCertChain: (List<X509Certificate>) -> PublicKey,
    ): T {
        val jws = JWSObject.parse(compactSerialization(jwsString))
        val certChain = checkNotNull(jws.header.x509CertChain) {
            "JWS has no certificate chain"
        }.map { X509Certificates.decode(it.toString()) }
        val publicKey = validateCertChain(certChain)
        val verifier = DefaultJWSVerifierFactory().createJWSVerifier(jws.header, publicKey)
        check(jws.verify(verifier)) {
            "JWS signature is invalid"
        }
        return payloadFormat.decodeFromString<T>(jws.payload.toString())
    }

    @PublishedApi
    internal fun compactSerialization(jws: String): String = if (jws.contains('.')) {
        jws
    } else {
        Base64URL(jws).decodeToString()
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
