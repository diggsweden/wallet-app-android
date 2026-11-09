// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.credentialdetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import se.digg.wallet.core.data.CredentialStore
import se.digg.wallet.core.data.extensions.getClaimUiModels
import timber.log.Timber

@HiltViewModel
class CredentialDetailsViewModel @Inject constructor(
    private val credentialStore: CredentialStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow<CredentialDetailsState>(CredentialDetailsState.Loading)
    val uiState: StateFlow<CredentialDetailsState> = _uiState.asStateFlow()

    fun loadCredential(credentialId: String) {
        viewModelScope.launch {
            _uiState.value = CredentialDetailsState.Loading
            try {
                val credential = credentialStore.getCredential(id = credentialId)
                val claims = credential.getClaimUiModels()
                _uiState.value = CredentialDetailsState.Credential(
                    claims = claims,
                    issuer = credential.issuer?.name,
                    issuerImgUrl = credential.issuer?.logo?.uri?.toString().orEmpty(),
                )
            } catch (e: Exception) {
                Timber.d("CredentialDetailsViewModel - Error: ${e.message}")
                _uiState.value = CredentialDetailsState.Error(
                    errorMessage =
                        "Error loading locally stored credential - Error: ${e.message}",
                )
            }
        }
    }
}
