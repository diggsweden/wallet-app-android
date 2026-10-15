// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.crypto

interface ProofSigner {
    suspend fun sign(keyId: ProofKeyId, data: ByteArray): String
}
