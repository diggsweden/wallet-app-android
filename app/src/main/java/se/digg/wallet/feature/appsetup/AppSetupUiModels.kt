// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.appsetup

sealed interface AppSetupUiState {
    object Loading : AppSetupUiState
    data class Error(val message: String?) : AppSetupUiState
    object Reset : AppSetupUiState
}

sealed interface AppSetupUiEffect {
    object NavigateToIntro : AppSetupUiEffect
    object NavigateToDashboard : AppSetupUiEffect
}
