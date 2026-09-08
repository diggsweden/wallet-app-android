// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.crypto

import com.nimbusds.jose.jwk.JWK
import javax.inject.Inject
import se.digg.wallet.access_mechanism.api.OpaqueClient
import se.digg.wallet.core.network.WalletOpaqueClient
import se.digg.wallet.core.services.KeyAlias
import se.digg.wallet.core.services.KeystoreManager
import se.digg.wallet.data.UserRepository

class HsmProofSigner @Inject constructor(
    private val userRepository: UserRepository,
    private val opaqueTransport: WalletOpaqueClient,
) : ProofSigner {

    override suspend fun publicKey(pin: String): JWK = authenticate(pin = pin).publicKey

    override suspend fun sign(pin: String, data: ByteArray): String {
        val session = authenticate(pin = pin)
        return session.opaqueClient.sign(kid = session.publicKey.keyID, data).signature
    }

    private suspend fun authenticate(pin: String): HsmSession {
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
        return HsmSession(opaqueClient = opaqueClient, publicKey = hsmKey.publicKey)
    }
}

private data class HsmSession(val opaqueClient: OpaqueClient, val publicKey: JWK)
