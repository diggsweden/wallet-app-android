package se.digg.wallet.feature.issuance

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import com.nimbusds.jose.JOSEObjectType
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

object KeystoreManager {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"

    fun getOrCreateEs256Key(alias: String, preferStrongBox: Boolean = true): KeyPair {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (ks.containsAlias(alias)) {
            val entry = runCatching {
                ks.getEntry(alias, null) as KeyStore.PrivateKeyEntry
            }.getOrNull()

            if (entry != null) {
                val pub = entry.certificate.publicKey as ECPublicKey
                val private: PrivateKey = entry.privateKey
                return KeyPair(pub, private)
            }
        }
        return generateEs256Key(alias, preferStrongBox)
    }

    private fun generateEs256Key(alias: String, preferStrongBox: Boolean): KeyPair {

        val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, ANDROID_KEYSTORE)

        // Try StrongBox first (if available)
        if (preferStrongBox && Build.VERSION.SDK_INT >= 28) {
            try {
                kpg.initialize(
                    KeyGenParameterSpec.Builder(
                        alias,
                        KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
                    )
                        .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1")) // P-256
                        .setDigests(KeyProperties.DIGEST_SHA256)
                        .setIsStrongBoxBacked(true)
                        .build()
                )
                return kpg.generateKeyPair()
            } catch (e: StrongBoxUnavailableException) {
                // fall back below
                Timber.d("Error: ${e.message}")
            } catch (e: Exception) {
                // some devices throw other exceptions; fall back
                Timber.d("Error: ${e.message}")
            }
        }

        // Fallback: normal Keystore
        kpg.initialize(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
            )
                .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                .setDigests(KeyProperties.DIGEST_SHA256)
                .build()
        )
        return kpg.generateKeyPair()
    }

    fun exportJwk(alias: String, keyPair: KeyPair): ECKey {
        //val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val publicKey = keyPair.public as? ECPublicKey
            ?: error("No key for alias '$alias'")

        val jwk: ECKey = ECKey.Builder(Curve.P_256, publicKey)
            .keyID(alias) // "kid": alias (or compute a thumbprint if you prefer)
            .build()
        return jwk
    }

    fun createJWT(
        keyPair: KeyPair,
        payload: Map<String, Any?>,
        headerType: String? = null
    ): String {
        val now = Instant.now().epochSecond.toInt()
        val claims = mapOf(
            "iat" to now,
            "nbf" to now,
            "exp" to now + 600
        ) + payload

        val exportedECKey = exportJwk("alias", keyPair)
        val publicECKey = exportedECKey.toPublicJWK()

        val header = JWSHeader.Builder(JWSAlgorithm.ES256)
            .apply { if (headerType != null) this.type(JOSEObjectType(headerType)) }
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
