// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.enrollment.fetchid

sealed interface FetchIdUiState {
    object Idle : FetchIdUiState
    object Loading : FetchIdUiState
    object Error : FetchIdUiState
}

sealed interface FetchIdUiEvent {
    object NextClicked : FetchIdUiEvent
    object FetchIdClicked : FetchIdUiEvent
}

sealed interface FetchIdUiEffect {
    object OnNext : FetchIdUiEffect
    data class OnCredentialOfferFetched(val credentialOffer: String) : FetchIdUiEffect
}
