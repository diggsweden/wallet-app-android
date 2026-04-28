// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

@file:Suppress("ktlint:standard:filename")

package se.digg.wallet.feature.credentialdetails

import se.digg.wallet.data.ClaimUiModel

sealed interface CredentialDetailsState {
    object Loading : CredentialDetailsState
    data class Credential(
        val claims: List<ClaimUiModel>,
        val issuer: String?,
        val issuerImgUrl: String,
    ) : CredentialDetailsState

    data class Error(val errorMessage: String) : CredentialDetailsState
}
