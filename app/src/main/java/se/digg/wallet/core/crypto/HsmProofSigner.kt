// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.crypto

import javax.inject.Inject
import se.digg.wallet.access_mechanism.api.OpaqueClient
import se.digg.wallet.access_mechanism.model.ServerParameters
import se.digg.wallet.core.network.WalletOpaqueClient
import se.digg.wallet.core.services.KeyAlias
import se.digg.wallet.core.services.KeystoreManager
import se.digg.wallet.data.UserRepository

class HsmProofSigner(
    private val serverParameters: suspend () -> ServerParameters,
    private val opaqueTransport: WalletOpaqueClient,
    private val pin: String,
) : ProofKeyManager {

    private var opaqueClient: OpaqueClient? = null

    override suspend fun sign(keyId: ProofKeyId, data: ByteArray): String =
        getClient().sign(keyId.value, data).signature

    override suspend fun createKey(): ProofKey {
        val key = getClient().createHsmKey().publicKey
        return ProofKey(
            id = ProofKeyId(
                checkNotNull(key.keyID) {
                    "HSM public key is missing kid"
                },
            ),
            publicKey = key.toECKey(),
        )
    }

    override suspend fun deleteKey(keyId: ProofKeyId) {
        getClient().deleteHsmKey(kid = keyId.value)
    }

    override suspend fun authenticate() {
        opaqueClient = makeAuthenticatedClient()
    }

    suspend fun getClient(): OpaqueClient =
        opaqueClient ?: makeAuthenticatedClient().also { client -> opaqueClient = client }

    suspend fun makeAuthenticatedClient(): OpaqueClient {
        val client = OpaqueClient.resume(
            transport = opaqueTransport,
            serverParameters = serverParameters(),
            clientKeyPair = KeystoreManager.getOrCreateEs256Key(KeyAlias.DEVICE_KEY),
            pinStretchPrivateKey = KeystoreManager.getPinStretchPrivateKey(),
        )
        client.authenticate(pin = pin)
        return client
    }
}

class HsmProofKeyManagerFactory @Inject constructor(
    private val userRepository: UserRepository,
    private val opaqueTransport: WalletOpaqueClient,
) : ProofKeyManagerFactory {
    override fun create(pin: String): ProofKeyManager = HsmProofSigner(
        serverParameters = userRepository::getServerParameters,
        opaqueTransport = opaqueTransport,
        pin = pin,
    )
}
