// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.onboarding.walletsetup

enum class SetupStep {
    CREATE_ACCOUNT,
    INIT_HSM,
    REGISTER_PIN,
    AUTHENTICATE,
    CREATE_HSM_KEY,
    POST_HSM_KEY,
}

sealed interface WalletSetupUiState {
    data class InProgress(val step: SetupStep) : WalletSetupUiState
    data class Failed(val step: SetupStep) : WalletSetupUiState
}

sealed interface WalletSetupUiEffect {
    object OnNext : WalletSetupUiEffect
}
