// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlin.collections.removeLastOrNull

class WalletNavigator(val backStack: NavBackStack<NavKey>) {
    fun navigate(key: NavKey) {
        backStack.add(key)
    }

    fun goBack() {
        backStack.removeLastOrNull()
    }

    fun resetToOnboarding() {
        backStack.clear()
        backStack.add(IntroKey)
    }

    fun resetToDashboard() {
        backStack.clear()
        backStack.add(DashboardKey)
    }
}
