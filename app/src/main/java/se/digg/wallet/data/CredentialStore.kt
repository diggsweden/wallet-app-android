// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.data

import se.digg.wallet.core.passkey.StoredPasskey

/**
 * The slice of the user's stored credentials that the issuance and presentation flows need.
 */
interface CredentialStore {
    suspend fun getCredentials(): List<SavedCredential>

    suspend fun addCredentials(credentials: List<SavedCredential>)
}

/**
 * Passkey PoC: the slice of the user's stored passkey state that the issuance
 * and presentation signing steps need.
 */
interface PasskeyStore {
    suspend fun getPasskey(): StoredPasskey?

    suspend fun setPasskey(passkey: StoredPasskey)

    suspend fun getEncryptedPin(): String?

    suspend fun setEncryptedPin(encryptedPin: String)
}

interface WuaProvider {
    /** Fetches a Wallet Unit Attestation, bound to [nonce] when one is given. */
    suspend fun fetchWua(nonce: String?): String
}
