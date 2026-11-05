// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.data

import javax.inject.Inject
import se.digg.wallet.core.crypto.HsmProofSigner
import se.digg.wallet.core.crypto.ProofKeyManager
import se.digg.wallet.core.crypto.ProofKeyManagerFactory
import se.digg.wallet.core.network.WalletOpaqueClient

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
