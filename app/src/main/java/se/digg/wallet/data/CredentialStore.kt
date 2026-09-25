// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.data

import com.nimbusds.jose.jwk.ECKey

/**
 * The slice of the user's stored credentials that the issuance and presentation flows need.
 */
interface CredentialStore {
    suspend fun getCredentials(): List<SavedCredential>

    suspend fun addCredentials(credentials: List<SavedCredential>)
}

interface KeyAttestationProvider {
    /** Fetches a Wallet Unit Attestation, bound to [nonce] when one is given. */
    suspend fun getKeyAttestation(keys: List<ECKey>, nonce: String?): String
}
