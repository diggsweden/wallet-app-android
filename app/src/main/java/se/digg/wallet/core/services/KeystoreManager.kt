// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2
package se.digg.wallet.core.services

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSObject
import com.nimbusds.jose.JWSSigner
import com.nimbusds.jose.Payload
import com.nimbusds.jose.crypto.impl.ECDSA
import com.nimbusds.jose.jca.JCAContext
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.util.Base64URL
import com.nimbusds.jose.util.JSONObjectUtils
import timber.log.Timber
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.time.Instant

enum class KeyAlias(val value: String) {
    DEVICE_KEY("device_key_alias"), WALLET_KEY("wallet_key_alias")
}

object KeystoreManager {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"

    fun getOrCreateEs256Key(alias: KeyAlias, tryStrongBox: Boolean = true): KeyPair {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (ks.containsAlias(alias.value)) {
            val entry = runCatching {
                ks.getEntry(alias.value, null) as KeyStore.PrivateKeyEntry
            }.getOrNull()

            if (entry != null) {
                val pub = entry.certificate.publicKey as ECPublicKey
                val private: PrivateKey = entry.privateKey
                return KeyPair(pub, private)
            }
        }
        return generateEs256Key(alias, tryStrongBox)
    }

    private fun generateEs256Key(alias: KeyAlias, tryStrongBox: Boolean): KeyPair {
        try {
            val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, ANDROID_KEYSTORE)
            kpg.initialize(
                KeyGenParameterSpec.Builder(
                    alias.value,
                    KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
                ).apply {
                    setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1")) // P-256
                    setDigests(KeyProperties.DIGEST_SHA256)
                    if (tryStrongBox) {
                        setIsStrongBoxBacked(true)
                    }
                }.build()
            )
            return kpg.generateKeyPair()
        } catch (e: StrongBoxUnavailableException) {
            Timber.d("Strongbox error: ${e.message}")
            return generateEs256Key(alias, tryStrongBox = false)
        } catch (e: Exception) {
            Timber.d("Error: ${e.message}")
            throw Exception("Kunde inte spara nyckel")
        }
    }

    fun exportJwk(keyPair: KeyPair): ECKey {
        val publicKey = keyPair.public as? ECPublicKey
            ?: error("No publicKey")

        val jwk: ECKey = ECKey.Builder(Curve.P_256, publicKey)
            .keyID("testar")
//            .keyIDFromThumbprint()
            .build()
        return jwk
    }

    fun createJWT(
        keyPair: KeyPair,
        payload: Map<String, Any?>,
        headers: Map<String, Any>
    ): String {
        val now = Instant.now().epochSecond.toInt()
        val claims = mapOf(
            "iat" to now,
            "nbf" to now,
            "exp" to now + 600
        ) + payload

        val exportedECKey = exportJwk(keyPair)
        val publicECKey = exportedECKey.toPublicJWK()

        val header = JWSHeader.Builder(JWSAlgorithm.ES256).customParams(headers)
            //.apply { this.type(JOSEObjectType(headers)) }
            .jwk(publicECKey)
            .build()

        val jws = JWSObject(
            header,
            Payload(JSONObjectUtils.toJSONString(claims))
        )

        jws.sign(object : JWSSigner {
            override fun sign(
                header: JWSHeader?,
                signingInput: ByteArray?
            ): Base64URL {
                val signature = Signature.getInstance("SHA256withECDSA").run {
                    initSign(keyPair.private)
                    update(signingInput)
                    sign()
                }
                val signatureByteArrayLength = ECDSA.getSignatureByteArrayLength(JWSAlgorithm.ES256)
                val joseSignature =
                    ECDSA.transcodeSignatureToConcat(signature, signatureByteArrayLength)
                return Base64URL.encode(joseSignature)
            }

            override fun supportedJWSAlgorithms(): Set<JWSAlgorithm?> {
                return setOf(JWSAlgorithm.ES256)
            }

            override fun getJCAContext(): JCAContext {
                return JCAContext()
            }
        }
        )

        return jws.serialize()
    }
}
