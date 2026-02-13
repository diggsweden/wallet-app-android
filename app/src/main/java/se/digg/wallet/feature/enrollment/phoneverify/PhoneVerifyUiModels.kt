// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.enrollment.phoneverify

data class PhoneVerifyUiState(
    val phone: String = "",
    val code: String = "",
    val showError: Boolean = false,
)

sealed interface PhoneVerifyUiEvent {
    data class CodeChanged(val value: String) : PhoneVerifyUiEvent
    object NextClicked : PhoneVerifyUiEvent
}

sealed interface PhoneVerifyUiEffect {
    object OnNext : PhoneVerifyUiEffect
}
