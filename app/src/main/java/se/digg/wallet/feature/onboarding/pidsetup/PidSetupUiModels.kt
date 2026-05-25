// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.onboarding.pidsetup

sealed interface PidSetupUiState {
    object Idle : PidSetupUiState
    object Loading : PidSetupUiState
    object Error : PidSetupUiState
}

sealed interface PidSetupUiEvent {
    object NextClicked : PidSetupUiEvent
    object PidSetupClicked : PidSetupUiEvent
}

sealed interface PidSetupUiEffect {
    object OnNext : PidSetupUiEffect
    data class OnCredentialOfferFetched(val credentialOffer: String) : PidSetupUiEffect
}
