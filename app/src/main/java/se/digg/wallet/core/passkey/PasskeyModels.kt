// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.passkey

import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import kotlinx.serialization.Serializable

/**
 * A passkey registered for this wallet, as returned by Credential Manager.
 *
 * PoC note: the public key is kept locally so assertions can be verified
 * on-device. In a production implementation registration and verification
 * would happen server-side (FIDO2/WebAuthn relying party).
 */
@Serializable
data class StoredPasskey(
    val credentialId: String,
    val publicKeySpkiB64: String,
    val userHandle: String,
    val type: PasskeyType = PasskeyType.PLATFORM,
)

/**
 * PLATFORM is a real Google Credential Manager passkey. LOCAL_BIOMETRIC is
 * the PoC fallback used when the relying party's assetlinks.json is not
 * deployed: an Android Keystore key gated by the system biometric prompt,
 * mimicking the passkey UX without the WebAuthn plumbing.
 */
@Serializable
enum class PasskeyType {
    PLATFORM,
    LOCAL_BIOMETRIC,
}

sealed interface PasskeyCreateResult {
    data class Success(val passkey: StoredPasskey) : PasskeyCreateResult
    data object Cancelled : PasskeyCreateResult
    data class Failed(val message: String?) : PasskeyCreateResult
}

/**
 * State for the passkey confirmation shown in place of the PIN prompt
 * (issuance and presentation signing steps).
 */
data class PasskeyConfirmUiState(
    val passkey: StoredPasskey? = null,
    val inProgress: Boolean = false,
    val error: String? = null,
)

sealed interface PasskeyAssertResult {
    data object Success : PasskeyAssertResult
    data object Cancelled : PasskeyAssertResult
    data class Failed(val message: String?) : PasskeyAssertResult
}

internal fun b64UrlEncode(bytes: ByteArray): String =
    Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

internal fun b64UrlDecode(value: String): ByteArray = Base64.getUrlDecoder().decode(value)

internal fun decodeSpkiPublicKey(spkiB64: String): PublicKey =
    KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(b64UrlDecode(spkiB64)))
