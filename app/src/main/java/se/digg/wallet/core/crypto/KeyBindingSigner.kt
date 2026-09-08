// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.crypto

import java.security.MessageDigest
import java.util.Base64
import javax.inject.Inject
import se.digg.wallet.data.KeybindingPayload

class KeyBindingSigner @Inject constructor(private val proofSigner: ProofSigner) {
    suspend fun sign(sdJwt: String, nonce: String, audience: String, pin: String): String {
        val payload = KeybindingPayload(
            aud = audience,
            nonce = nonce,
            sdHash = sdJwtHash(sdJwt),
        )
        val headers = mapOf<String, Any>("typ" to "kb+jwt")

        return JwtUtils.signJwtWith(payload, headers) { data ->
            proofSigner.sign(pin = pin, data = data)
        }
    }

    private fun sdJwtHash(sdJwt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(sdJwt.toByteArray(Charsets.US_ASCII))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash)
    }
}
