package se.digg.wallet.core.crypto

import com.nimbusds.jose.Algorithm
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
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.JWK
import java.security.KeyPair
import java.security.interfaces.ECPublicKey
import java.time.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

object JwtUtils {
    fun exportJwk(keyPair: KeyPair, algorithm: Algorithm = JWEAlgorithm.ECDH_ES): ECKey {
        val publicKey = keyPair.public as? ECPublicKey
            ?: error("No publicKey")

        val jwk: ECKey = ECKey.Builder(Curve.P_256, publicKey).apply {
            algorithm(algorithm)
            keyIDFromThumbprint()
        }.build()
        return jwk
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
    ): String {
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

        val exportedECKey = exportJwk(keyPair, algorithm)
        val publicECKey = exportedECKey.toPublicJWK()

        val header = JWSHeader.Builder(algorithm).customParams(headers).apply {
            if (includeJwk) {
                jwk(publicECKey)
            }
        }.build()

        val jws = JWSObject(
            header,
            Payload(encoded),
        )

        jws.sign(WalletSigner(keyPair))

        return jws.serialize()
    }
}
