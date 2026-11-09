// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.credentialdetails

import androidx.compose.runtime.Immutable
import se.digg.wallet.core.model.ClaimUiModel

@Immutable
sealed interface CredentialDetailsState {
    data object Loading : CredentialDetailsState

    data class Credential(
        val claims: List<ClaimUiModel>,
        val issuer: String?,
        val issuerImgUrl: String,
    ) : CredentialDetailsState

    data class Error(val errorMessage: String) : CredentialDetailsState
}
