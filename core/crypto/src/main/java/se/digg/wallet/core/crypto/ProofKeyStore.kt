// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.crypto

interface ProofKeyStore {
    suspend fun createKey(): ProofKey
    suspend fun deleteKey(keyId: ProofKeyId)
    suspend fun authenticate()
}
