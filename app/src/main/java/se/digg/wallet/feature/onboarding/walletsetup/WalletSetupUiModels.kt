// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.onboarding.walletsetup

import se.digg.wallet.core.error.AppError

enum class SetupStep {
    CREATE_ACCOUNT,
    INIT_HSM,
    REGISTER_PIN,
    AUTHENTICATE,
    POST_HSM_KEY,
}

/**
 * [problem] carries the full backend error body (status, type, detail, transactionId,
 * invalidParameters) when the failure came back as a structured RFC 9457 problem response.
 * It's null for every other failure shape (plain text, connectivity, unexpected) - screens
 * that only care about [title]/[message] never need to touch it.
 */
data class ErrorUiModel(
    val title: String?,
    val message: String?,
    val problem: AppError.Problem?,
)

sealed interface WalletSetupUiState {
    data class InProgress(val step: SetupStep) : WalletSetupUiState
    data class Failed(val step: SetupStep, val error: ErrorUiModel) : WalletSetupUiState
}

sealed interface WalletSetupUiEffect {
    object OnNext : WalletSetupUiEffect
}
