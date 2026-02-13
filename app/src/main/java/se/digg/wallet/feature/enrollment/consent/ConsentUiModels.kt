// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.enrollment.consent

data class ConsentUiState(val hasConsent: Boolean = false, val showError: Boolean = false)

sealed interface ConsentUiEvent {
    data class ConsentChanged(val isChecked: Boolean) : ConsentUiEvent
    object NextClicked : ConsentUiEvent
}

sealed interface ConsentUiEffect {
    object OnNext : ConsentUiEffect
}
