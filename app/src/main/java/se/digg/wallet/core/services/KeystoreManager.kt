// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2
package se.digg.wallet.core.services

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import timber.log.Timber

enum class KeyAlias(val value: String) {
    DEVICE_KEY("device_key_alias"),
    WALLET_KEY("wallet_key_alias"),
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
                    KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY,
                ).apply {
                    setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1")) // P-256
                    setDigests(KeyProperties.DIGEST_SHA256)
                    if (tryStrongBox) {
                        setIsStrongBoxBacked(true)
                    }
                }.build(),
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

    fun createSoftwareEcdhKey(): KeyPair {
        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(ECGenParameterSpec("secp256r1"))
        return kpg.generateKeyPair()
    }
}
