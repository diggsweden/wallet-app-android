// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.crypto

import java.security.MessageDigest
import java.util.Base64
import javax.inject.Inject
import se.digg.wallet.access_mechanism.api.OpaqueClient
import se.digg.wallet.core.network.WalletOpaqueClient
import se.digg.wallet.core.services.KeyAlias
import se.digg.wallet.core.services.KeystoreManager
import se.digg.wallet.data.KeybindingPayload
import se.digg.wallet.data.UserRepository

class KeyBindingSigner @Inject constructor(
    private val userRepository: UserRepository,
    private val opaqueTransport: WalletOpaqueClient,
) {
    suspend fun sign(sdJwt: String, nonce: String, audience: String, pin: String): String {
        val opaqueClient = OpaqueClient.resume(
            transport = opaqueTransport,
            serverParameters = checkNotNull(userRepository.getServerParameters()) {
                "Missing OPAQUE server parameters"
            },
            clientKeyPair = KeystoreManager.getOrCreateEs256Key(KeyAlias.DEVICE_KEY),
            pinStretchPrivateKey = KeystoreManager.getPinStretchPrivateKey(),
        )
        opaqueClient.authenticate(pin = pin)

        val hsmKey = checkNotNull(opaqueClient.listHsmKeys().firstOrNull()) {
            "No HSM keys found"
        }

        val payload = KeybindingPayload(
            aud = audience,
            nonce = nonce,
            sdHash = sdJwtHash(sdJwt),
        )
        val headers = mapOf<String, Any>("typ" to "kb+jwt")

        return JwtUtils.signJwtWith(payload, headers) { data ->
            opaqueClient.sign(kid = hsmKey.publicKey.keyID, data).signature
        }
    }

    private fun sdJwtHash(sdJwt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(sdJwt.toByteArray(Charsets.US_ASCII))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash)
    }
}
