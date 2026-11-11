// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.appsetup

sealed interface AppSetupUiEffect {
    data object NavigateToIntro : AppSetupUiEffect
    data object NavigateToDashboard : AppSetupUiEffect
}
