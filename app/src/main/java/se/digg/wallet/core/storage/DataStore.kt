// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.storage

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import se.digg.wallet.data.CredentialData

private const val DS_NAME = "credential_prefs"

val Context.credentialDataStore by preferencesDataStore(DS_NAME)

object CredentialStore {
    private val KEY_JWT = stringPreferencesKey("jwt")

    /** Continuous stream of Credential? (null if absent) */
    fun credentialFlow(context: Context): Flow<CredentialData?> =
        context.credentialDataStore.data.map { prefs ->
            prefs[KEY_JWT]?.let { jwt -> CredentialData(jwt) }
        }

    /** One-shot read (suspending). */
    suspend fun getCredential(context: Context): CredentialData? =
        credentialFlow(context).first()

    /** Save/overwrite credential. */
    suspend fun saveCredential(context: Context, credential: CredentialData) {
        context.credentialDataStore.edit { it[KEY_JWT] = credential.jwt }
    }

    /** Clear credential. */
    suspend fun clearCredential(context: Context) {
        context.credentialDataStore.edit { it.remove(KEY_JWT) }
    }
}
