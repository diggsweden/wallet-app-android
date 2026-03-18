// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.presentation

import se.digg.wallet.data.ClaimUiModel

sealed interface PresentationUiState {
    object Loading : PresentationUiState
    data class PresentClaims(val claims: List<ClaimUiModel>) : PresentationUiState
    object ShareSuccess : PresentationUiState
    data class Error(val message: String?) : PresentationUiState
}

sealed interface PresentationUiEffect {
    data class OpenUrl(val url: String) : PresentationUiEffect
}
