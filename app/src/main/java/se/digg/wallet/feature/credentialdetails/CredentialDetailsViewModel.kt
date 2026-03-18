// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.credentialdetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import se.digg.wallet.core.extensions.getClaimUiModels
import se.digg.wallet.data.ClaimUiModel
import se.digg.wallet.data.UserRepository
import timber.log.Timber

sealed interface CredentialDetailsState {
    object Loading : CredentialDetailsState
    data class Credential(
        val claims: List<ClaimUiModel>,
        val issuer: String?,
        val issuerImgUrl: String,
    ) : CredentialDetailsState

    data class Error(val errorMessage: String) : CredentialDetailsState
}

@HiltViewModel
class CredentialDetailsViewModel @Inject constructor(private val userRepository: UserRepository) :
    ViewModel() {

    private val _uiState = MutableStateFlow<CredentialDetailsState>(CredentialDetailsState.Loading)
    val uiState: StateFlow<CredentialDetailsState> = _uiState

    fun matchDisclosures() {
        viewModelScope.launch {
            try {
                val credential =
                    checkNotNull(userRepository.getCredential()) { "No credential found" }
                val claims = credential.getClaimUiModels()
                _uiState.value = CredentialDetailsState.Credential(
                    claims = claims,
                    issuer = credential.issuer?.name,
                    issuerImgUrl = credential.issuer?.logo?.uri?.toString() ?: "",
                )
            } catch (e: Exception) {
                Timber.d("CredentialDetailsViewModel - Error: ${e.message}")
                _uiState.value =
                    CredentialDetailsState.Error(
                        errorMessage =
                            "Error with loading locally stored PID - Error: ${e.message}",
                    )
            }
        }
    }
}
