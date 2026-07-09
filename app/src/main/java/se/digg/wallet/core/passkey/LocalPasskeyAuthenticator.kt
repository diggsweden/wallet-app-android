// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.passkey

import android.content.Context
import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.util.UUID
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import se.digg.wallet.R
import timber.log.Timber

/**
 * PoC fallback for [PasskeyManager]: emulates the passkey UX with an Android
 * Keystore ES256 key that can only sign after the system biometric prompt.
 * Used when the relying party's assetlinks.json is not deployed, so the demo
 * works on any device without server-side changes. Unlike a real passkey it
 * is device-bound (no Google Password Manager sync).
 */
internal class LocalPasskeyAuthenticator {

    suspend fun createPasskey(activityContext: Context): PasskeyCreateResult {
        val alias = "$ALIAS_PREFIX${UUID.randomUUID()}"
        return try {
            val keyPair = generateBiometricKey(alias)
            val challenge = randomChallenge()
            when (val signed = signWithBiometrics(activityContext, alias, challenge)) {
                is LocalSignResult.Success -> {
                    val publicKeySpkiB64 = b64UrlEncode(keyPair.public.encoded)
                    if (!verify(publicKeySpkiB64, challenge, signed.signature)) {
                        deleteKey(alias)
                        return PasskeyCreateResult.Failed("Signature verification failed")
                    }
                    PasskeyCreateResult.Success(
                        StoredPasskey(
                            credentialId = alias,
                            publicKeySpkiB64 = publicKeySpkiB64,
                            userHandle = b64UrlEncode(
                                ByteArray(16).also { SecureRandom().nextBytes(it) },
                            ),
                            type = PasskeyType.LOCAL_BIOMETRIC,
                        ),
                    )
                }

                LocalSignResult.Cancelled -> {
                    deleteKey(alias)
                    PasskeyCreateResult.Cancelled
                }

                is LocalSignResult.Failed -> {
                    deleteKey(alias)
                    PasskeyCreateResult.Failed(signed.message)
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Local passkey creation failed")
            deleteKey(alias)
            PasskeyCreateResult.Failed(e.message)
        }
    }

    suspend fun assertPasskey(
        activityContext: Context,
        passkey: StoredPasskey,
    ): PasskeyAssertResult = try {
        val challenge = randomChallenge()
        when (
            val signed = signWithBiometrics(activityContext, passkey.credentialId, challenge)
        ) {
            is LocalSignResult.Success -> {
                if (verify(passkey.publicKeySpkiB64, challenge, signed.signature)) {
                    PasskeyAssertResult.Success
                } else {
                    PasskeyAssertResult.Failed("Signature verification failed")
                }
            }

            LocalSignResult.Cancelled -> {
                PasskeyAssertResult.Cancelled
            }

            is LocalSignResult.Failed -> {
                PasskeyAssertResult.Failed(signed.message)
            }
        }
    } catch (e: Exception) {
        Timber.w(e, "Local passkey assertion failed")
        PasskeyAssertResult.Failed(e.message)
    }

    private sealed interface LocalSignResult {
        data class Success(val signature: ByteArray) : LocalSignResult
        data object Cancelled : LocalSignResult
        data class Failed(val message: String?) : LocalSignResult
    }

    /**
     * The key requires user authentication per use, so signing only completes
     * through the BiometricPrompt CryptoObject flow.
     */
    private suspend fun signWithBiometrics(
        activityContext: Context,
        alias: String,
        data: ByteArray,
    ): LocalSignResult = withContext(Dispatchers.Main) {
        val privateKey = loadKey(alias)
            ?: return@withContext LocalSignResult.Failed("Passkey key not found on this device")
        val signature = Signature.getInstance(SIGNATURE_ALGORITHM).apply {
            initSign(privateKey)
        }
        suspendCancellableCoroutine<LocalSignResult> { continuation ->
            val cancellationSignal = CancellationSignal()
            continuation.invokeOnCancellation { cancellationSignal.cancel() }
            val executor = activityContext.mainExecutor

            val prompt = BiometricPrompt.Builder(activityContext)
                .setTitle(activityContext.getString(R.string.passkey_local_prompt_title))
                .setSubtitle(activityContext.getString(R.string.passkey_local_prompt_subtitle))
                .setNegativeButton(
                    activityContext.getString(R.string.generic_cancel),
                    executor,
                ) { _, _ ->
                    if (continuation.isActive) continuation.resume(LocalSignResult.Cancelled)
                }
                .build()

            prompt.authenticate(
                BiometricPrompt.CryptoObject(signature),
                cancellationSignal,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(
                        result: BiometricPrompt.AuthenticationResult,
                    ) {
                        if (!continuation.isActive) return
                        val authorizedSignature = result.cryptoObject?.signature ?: signature
                        val signedBytes = try {
                            authorizedSignature.update(data)
                            authorizedSignature.sign()
                        } catch (e: Exception) {
                            continuation.resume(LocalSignResult.Failed(e.message))
                            return
                        }
                        continuation.resume(LocalSignResult.Success(signedBytes))
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                        if (!continuation.isActive) return
                        val cancelled = errorCode == BiometricPrompt.BIOMETRIC_ERROR_CANCELED ||
                            errorCode == BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED
                        continuation.resume(
                            if (cancelled) {
                                LocalSignResult.Cancelled
                            } else {
                                LocalSignResult.Failed(errString?.toString())
                            },
                        )
                    }
                },
            )
        }
    }

    private fun verify(
        publicKeySpkiB64: String,
        challenge: ByteArray,
        signatureBytes: ByteArray,
    ): Boolean = Signature.getInstance(SIGNATURE_ALGORITHM).run {
        initVerify(decodeSpkiPublicKey(publicKeySpkiB64))
        update(challenge)
        verify(signatureBytes)
    }

    private fun generateBiometricKey(alias: String): KeyPair {
        val generator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_EC,
            ANDROID_KEYSTORE,
        )
        generator.initialize(
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN)
                .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setUserAuthenticationRequired(true)
                .setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
                .build(),
        )
        return generator.generateKeyPair()
    }

    private fun loadKey(alias: String): PrivateKey? {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return keyStore.getKey(alias, null) as? PrivateKey
    }

    private fun deleteKey(alias: String) {
        runCatching {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (keyStore.containsAlias(alias)) keyStore.deleteEntry(alias)
        }
    }

    private fun randomChallenge(): ByteArray = ByteArray(32).also { SecureRandom().nextBytes(it) }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val ALIAS_PREFIX = "passkey_local_"
        const val SIGNATURE_ALGORITHM = "SHA256withECDSA"
    }
}
