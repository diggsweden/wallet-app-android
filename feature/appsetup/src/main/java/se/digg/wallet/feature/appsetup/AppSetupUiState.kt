// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.appsetup

import androidx.compose.runtime.Immutable

@Immutable
sealed interface AppSetupUiState {
    data object Loading : AppSetupUiState
    data class Error(val message: String?) : AppSetupUiState
    data object Reset : AppSetupUiState
}
