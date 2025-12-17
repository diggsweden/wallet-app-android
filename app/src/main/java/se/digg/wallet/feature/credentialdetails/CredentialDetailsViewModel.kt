// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.credentialdetails

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import se.digg.wallet.core.storage.CredentialStore
import se.digg.wallet.data.CredentialLocal
import se.digg.wallet.data.DisclosureLocal
import timber.log.Timber

sealed interface CredentialDetailsState {
    object Loading : CredentialDetailsState
    data class Disclosures(val disclosures: List<DisclosureLocal>, val issuer: String?) : CredentialDetailsState
    data class Error(val errorMessage: String) : CredentialDetailsState
}

class CredentialDetailsViewModel(app: Application) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow<CredentialDetailsState>(CredentialDetailsState.Loading)
    val uiState: StateFlow<CredentialDetailsState> = _uiState

    fun matchDisclosures() {
        viewModelScope.launch {
            try {
                val storedCredential = CredentialStore.getCredential(
                    getApplication()
                )
                val credential: CredentialLocal = storedCredential?.jwt?.let {
                    return@let Json.decodeFromString(CredentialLocal.serializer(), it)
                } ?: return@launch
                val disclosures = credential.disclosures.values.toList()
                _uiState.value = CredentialDetailsState.Disclosures(disclosures = disclosures, issuer = credential.issuer?.name)
            } catch (e: Exception) {
                Timber.d("CredentialDetailsViewModel - Error: ${e.message}")
                _uiState.value =
                    CredentialDetailsState.Error(errorMessage = "Error with loading locally stored PID - Error: ${e.message}")
            }
        }
    }
}