// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.presentation

import se.digg.wallet.core.data.model.VerifierUiModel
import se.digg.wallet.core.model.ClaimUiModel

sealed interface PresentationUiState {
    object Loading : PresentationUiState
    data class PresentClaims(
        val verifier: VerifierUiModel,
        val certificate: CertificateDetails,
        val requiredCredentials: List<CredentialUiModel>,
        val optionalCredentials: List<CredentialUiModel>,
        val isEncrypted: Boolean,
    ) : PresentationUiState {
        val sharedCredentials: List<CredentialUiModel>
            get() = requiredCredentials + optionalCredentials.filter { it.isChecked }
    }

    data class EnterPin(val isWrongPin: Boolean = false) : PresentationUiState
    object Sharing : PresentationUiState
    data class ShareSuccess(
        val verifierName: String,
        val sharedCredentials: List<CredentialUiModel>,
    ) : PresentationUiState

    data class Error(val message: String?) : PresentationUiState
}

data class CredentialUiModel(
    val id: String,
    val name: String,
    val issuerName: String?,
    val logoUrl: String?,
    val claims: List<ClaimUiModel>,
    val isChecked: Boolean = false,
)

sealed interface PresentationUiEffect {
    data class OpenUrl(val url: String) : PresentationUiEffect
}
