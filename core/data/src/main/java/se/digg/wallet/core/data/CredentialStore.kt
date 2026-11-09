// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.data

import com.nimbusds.jose.jwk.ECKey
import kotlinx.coroutines.flow.Flow
import se.digg.wallet.core.model.SavedCredential

interface CredentialStore {
    val credentials: Flow<List<SavedCredential>>

    suspend fun getCredentials(): List<SavedCredential>

    suspend fun getCredential(id: String): SavedCredential =
        getCredentials().firstOrNull { it.id == id }
            ?: throw NoSuchElementException("Credential with id: $id not found")

    suspend fun addCredentials(credentials: List<SavedCredential>)
}

interface KeyAttestationProvider {
    suspend fun getKeyAttestation(keys: List<ECKey>, nonce: String?): String
}

interface WalletInstanceAttestationProvider {
    suspend fun getWalletInstanceAttestation(key: ECKey): String
}
