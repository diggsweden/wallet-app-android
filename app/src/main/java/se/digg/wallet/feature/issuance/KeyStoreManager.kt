package se.digg.wallet.feature.issuance

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec

object KeychainManager {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"

    /**
     * Generate a new ES256 (P-256) key under the given alias, replacing any existing one.
     * Returns the generated KeyPair (private stays in Keystore, public is exportable).
     */
    @Throws(Exception::class)
    fun generateEs256Key(alias: String, useStrongBoxIfAvailable: Boolean = true): KeyPair {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

        if (ks.containsAlias(alias)) {
            ks.deleteEntry(alias)
        }

        val params = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
            .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
            .setDigests(KeyProperties.DIGEST_SHA256)
            // .setUserAuthenticationRequired(true)                   // require PIN/biometric
            //.setUserAuthenticationParameters(123, KeyProperties.AUTH_BIOMETRIC_STRONG)
            .build()
            .let { base ->
                if (useStrongBoxIfAvailable) {
                    try {
                        KeyGenParameterSpec.Builder("",0).setIsStrongBoxBacked(true).build()
                    } catch (_: Exception) {
                        base
                    }
                } else base
            }

        val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, ANDROID_KEYSTORE)
        kpg.initialize(params)
        return kpg.generateKeyPair()
    }

    /** Fetch the existing private key if you need it (non-exportable bytes, but usable for signing). */
    @Throws(Exception::class)
    fun getPrivateKey(alias: String): PrivateKey? {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return (ks.getKey(alias, null) as? PrivateKey)
    }

    /** Fetch the existing public key (exportable). */
    @Throws(Exception::class)
    fun getPublicKey(alias: String): ECPublicKey? {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return ks.getCertificate(alias)?.publicKey as? ECPublicKey
    }
}
