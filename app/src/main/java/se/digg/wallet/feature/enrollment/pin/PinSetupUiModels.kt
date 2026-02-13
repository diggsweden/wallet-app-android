// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.enrollment.pin

data class PinSetupUiState(val pin: String = "")

sealed interface PinSetupUiEvent {
    object NextClicked : PinSetupUiEvent
}

sealed interface PinSetupUiEffect {
    object OnNext : PinSetupUiEffect
    object OnGoBack : PinSetupUiEffect
}
