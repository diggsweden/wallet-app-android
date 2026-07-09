// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2
package se.digg.wallet.core.services

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

enum class KeyAlias(val value: String) {
    DEVICE_KEY("device_key_alias"),
    WALLET_KEY("wallet_key_alias"),

    PIN_KEY("pin_key_alias"),

    PASSKEY_PIN_WRAP_KEY("passkey_pin_wrap_key_alias"),
}

/**
 * Android Keystore operations are blocking binder calls, so every public function
 * is main-safe: it suspends and runs the keystore work on [Dispatchers.IO].
 */
object KeystoreManager {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val PIN_WRAP_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val PIN_WRAP_IV_LENGTH = 12

    suspend fun getOrCreateEs256Key(alias: KeyAlias, tryStrongBox: Boolean = true): KeyPair =
        withContext(Dispatchers.IO) {
            val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
                load(null)
            }

            val entry = runCatching {
                ks.getEntry(alias.value, null) as KeyStore.PrivateKeyEntry
            }.getOrNull()

            if (entry != null) {
                warnIfKeyAgreementUnsupported(entry.privateKey, alias)
                KeyPair(entry.certificate.publicKey as ECPublicKey, entry.privateKey)
            } else {
                generateEs256Key(alias, tryStrongBox)
            }
        }

    fun generateEs256Key(alias: KeyAlias, tryStrongBox: Boolean = true): KeyPair {
        try {
            val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, ANDROID_KEYSTORE)
            kpg.initialize(
                KeyGenParameterSpec.Builder(
                    alias.value,
                    KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY or
                        KeyProperties.PURPOSE_AGREE_KEY,
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

    fun removeKey() {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)
        }

        if (keyStore.containsAlias(KeyAlias.DEVICE_KEY.value)) {
            keyStore.deleteEntry(KeyAlias.DEVICE_KEY.value)
        }
    }

    suspend fun createSoftwareEcdhKey(): KeyPair = withContext(Dispatchers.Default) {
        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(ECGenParameterSpec("secp256r1"))
        kpg.generateKeyPair()
    }

    suspend fun getPinStretchPrivateKey(): PrivateKey = withContext(Dispatchers.IO) {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
            load(null)
        }

        if (!keyStore.containsAlias(KeyAlias.PIN_KEY.value)) {
            val generator = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_EC,
                ANDROID_KEYSTORE,
            )
            val parameterSpec = KeyGenParameterSpec.Builder(
                KeyAlias.PIN_KEY.value,
                KeyProperties.PURPOSE_AGREE_KEY,
            ).setAlgorithmParameterSpec(
                ECGenParameterSpec("secp256r1"),
            ).build()

            generator.initialize(parameterSpec)
            generator.generateKeyPair()
        }
        keyStore.getKey(KeyAlias.PIN_KEY.value, null) as PrivateKey
    }

    /**
     * Passkey PoC: the OPAQUE flow still needs the raw PIN, so it is kept
     * locally wrapped by a keystore AES-GCM key and released after a passkey
     * assertion. A production passkey design would remove the stored PIN.
     */
    suspend fun encryptPin(pin: String): String = withContext(Dispatchers.IO) {
        val cipher = Cipher.getInstance(PIN_WRAP_TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, getOrCreatePinWrapKey())
        }
        val encrypted = cipher.doFinal(pin.toByteArray(Charsets.UTF_8))
        Base64.getEncoder().encodeToString(cipher.iv + encrypted)
    }

    suspend fun decryptPin(blob: String): String = withContext(Dispatchers.IO) {
        val bytes = Base64.getDecoder().decode(blob)
        val iv = bytes.copyOfRange(0, PIN_WRAP_IV_LENGTH)
        val encrypted = bytes.copyOfRange(PIN_WRAP_IV_LENGTH, bytes.size)
        val cipher = Cipher.getInstance(PIN_WRAP_TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, getOrCreatePinWrapKey(), GCMParameterSpec(128, iv))
        }
        cipher.doFinal(encrypted).toString(Charsets.UTF_8)
    }

    private fun getOrCreatePinWrapKey(): SecretKey {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (ks.getKey(KeyAlias.PASSKEY_PIN_WRAP_KEY.value, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE,
        )
        generator.init(
            KeyGenParameterSpec.Builder(
                KeyAlias.PASSKEY_PIN_WRAP_KEY.value,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build(),
        )
        return generator.generateKey()
    }

    /**
     * Keys generated before PURPOSE_AGREE_KEY was added keep their original purposes,
     * so ECDH against them fails. Surfaces the problem until a migration is decided.
     */
    private fun warnIfKeyAgreementUnsupported(privateKey: PrivateKey, alias: KeyAlias) {
        try {
            val factory = KeyFactory.getInstance(privateKey.algorithm, ANDROID_KEYSTORE)
            val keyInfo = factory.getKeySpec(privateKey, KeyInfo::class.java)
            if (keyInfo.purposes and KeyProperties.PURPOSE_AGREE_KEY == 0) {
                Timber.w(
                    "Key '%s' lacks PURPOSE_AGREE_KEY; key agreement with it will fail " +
                        "until the key is regenerated",
                    alias.value,
                )
            }
        } catch (e: Exception) {
            Timber.d(e, "Could not inspect purposes of key '${alias.value}'")
        }
    }
}
