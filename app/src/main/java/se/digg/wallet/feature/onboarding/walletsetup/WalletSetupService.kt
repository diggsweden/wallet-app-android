// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.onboarding.walletsetup

import com.nimbusds.jose.jwk.JWK
import javax.inject.Inject
import se.digg.wallet.access_mechanism.api.OpaqueClient
import se.digg.wallet.core.extensions.toECKey
import se.digg.wallet.core.network.WalletOpaqueClient
import se.digg.wallet.core.services.KeyAlias
import se.digg.wallet.core.services.KeystoreManager
import se.digg.wallet.data.UserRepository
import se.wallet.client.gateway.models.CreateAccountRequest
import se.wallet.client.gateway.models.EcJwkRequest

// TODO: Replace with real user data once onboarding collects it.
private const val PLACEHOLDER_PERSONAL_IDENTITY_NUMBER = "123456789"
private const val PLACEHOLDER_EMAIL = "test@test.test"
private const val PLACEHOLDER_TELEPHONE_NUMBER = "123456789"

interface WalletSetupService {
    suspend fun createAccount()
    suspend fun initHsm()
    suspend fun registerPin(pin: String)
    suspend fun authenticate(pin: String)
    suspend fun postHsmKey()
}

internal class DefaultWalletSetupService @Inject constructor(
    private val userRepository: UserRepository,
    private val opaqueTransport: WalletOpaqueClient,
) : WalletSetupService {

    private var opaqueClient: OpaqueClient? = null

    override suspend fun createAccount() {
        val keyPair = KeystoreManager.getOrCreateEs256Key(KeyAlias.DEVICE_KEY)
        val ecKey = keyPair.toECKey(withThumbprint = true)
        val accountId = userRepository.createAccount(
            CreateAccountRequest(
                personalIdentityNumber = PLACEHOLDER_PERSONAL_IDENTITY_NUMBER,
                emailAdress = PLACEHOLDER_EMAIL,
                telephoneNumber = PLACEHOLDER_TELEPHONE_NUMBER,
                deviceKey = EcJwkRequest(
                    kty = ecKey.keyType.value,
                    crv = ecKey.curve.name,
                    x = ecKey.x.toString(),
                    y = ecKey.y.toString(),
                    kid = ecKey.keyID,
                ),
            ),
        )
        userRepository.setAccountId(accountId)
    }

    override suspend fun initHsm() {
        val client = OpaqueClient.create(
            clientKeyPair = KeystoreManager.getOrCreateEs256Key(KeyAlias.DEVICE_KEY),
            pinStretchPrivateKey = KeystoreManager.getPinStretchPrivateKey(),
            transport = opaqueTransport,
        )
        opaqueClient = client
        userRepository.saveServerParameters(client.serverParameters)
    }

    override suspend fun registerPin(pin: String) {
        requireClient().registration(pin = pin)
    }

    override suspend fun authenticate(pin: String) {
        requireClient().authenticate(pin = pin)
    }

    override suspend fun postHsmKey() {
        val hsmKey = checkNotNull(requireClient().listHsmKeys().firstOrNull()) {
            "No HSM keys found"
        }
        val key = hsmKey.publicKey.toKeyRequest()
        userRepository.postWalletKey(request = key)
    }

    private fun requireClient(): OpaqueClient = checkNotNull(opaqueClient) {
        "OpaqueClient not initialized - initHsm must run first"
    }

    private fun JWK.toKeyRequest(): EcJwkRequest {
        val ecKey = toECKey()
        return EcJwkRequest(
            kty = ecKey.keyType.value,
            crv = ecKey.curve.name,
            x = ecKey.x.toString(),
            y = ecKey.y.toString(),
            kid = ecKey.keyID,
        )
    }
}
