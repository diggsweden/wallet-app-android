package se.digg.wallet.feature.issuance

// KeystoreManager.kt
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.jwk.KeyUse
import java.security.interfaces.ECPrivateKey

object KeystoreManager {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"

    /** Get or create a P-256 (ES256) keypair under [alias]. */
    fun getOrCreateEs256Key(alias: String, preferStrongBox: Boolean = true): KeyPair {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (ks.containsAlias(alias)) {
            val cert = ks.getCertificate(alias)
            val privateKey = ks.getKey(alias, null)
            @Suppress("UNCHECKED_CAST")
            return KeyPair(cert.publicKey as ECPublicKey, privateKey as java.security.PrivateKey)
        }
        return generateEs256Key(alias, preferStrongBox)
    }

    private fun generateEs256Key(alias: String, preferStrongBox: Boolean): KeyPair {
        val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC)

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
            } catch (_: StrongBoxUnavailableException) {
                // fall back below
            } catch (_: Exception) {
                // some devices throw other exceptions; fall back
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

    fun exportPublicJwk(alias: String): JWK {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val publicKey = ks.getCertificate(alias)?.publicKey as? ECPublicKey
            ?: error("No key for alias '$alias'")

        val jwk: ECKey = ECKey.Builder(Curve.P_256, publicKey)
            .keyUse(KeyUse.SIGNATURE)       // "use": "sig"
            .algorithm(JWSAlgorithm.ES256)   // "alg": "ES256"
            .keyID(alias)                    // "kid": alias (or compute a thumbprint if you prefer)
            .build()
        return jwk
    }

    fun exportPrivateKey(alias: String): ECPrivateKey? {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return (ks.getKey(alias, null) as? ECPrivateKey)
    }

    fun generateEcKey(alias: String = "my_ec_key"): Pair<ECPublicKey, ECPrivateKey> {
        val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore")
        kpg.initialize(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
            )
                .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1")) // P-256 (ES256)
                .setDigests(KeyProperties.DIGEST_SHA256)
                .build()
        )
        val kp = kpg.generateKeyPair()
        return kp.public as ECPublicKey to kp.private as ECPrivateKey
    }
}
