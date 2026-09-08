// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.crypto

import com.nimbusds.jose.jwk.JWK

interface ProofSigner {
    suspend fun publicKey(pin: String): JWK

    suspend fun sign(pin: String, data: ByteArray): String
}
