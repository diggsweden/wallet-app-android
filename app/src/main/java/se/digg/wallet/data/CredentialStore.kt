// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.data

/**
 * The slice of the user's stored credentials that the issuance and presentation flows need.
 */
interface CredentialStore {
    suspend fun getCredentials(): List<SavedCredential>

    suspend fun addCredentials(credentials: List<SavedCredential>)
}

interface WuaProvider {
    /** Fetches a Wallet Unit Attestation, bound to [nonce] when one is given. */
    suspend fun fetchWua(nonce: String?): String
}
