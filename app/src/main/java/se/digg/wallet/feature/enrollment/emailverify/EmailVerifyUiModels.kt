// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.enrollment.emailverify

data class EmailVerifyUiState(
    val code: String = "",
    val showError: Boolean = false,
    val email: String = "",
)

sealed interface EmailVerifyUiEvent {
    data class CodeChanged(val value: String) : EmailVerifyUiEvent
    object NextClicked : EmailVerifyUiEvent
}

sealed interface EmailVerifyUiEffect {
    object OnNext : EmailVerifyUiEffect
}
