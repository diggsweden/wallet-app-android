// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WalletNavigatorTest {

    @Test
    fun `navigate adds key to backstack`() {
        val backStack = NavBackStack<NavKey>(AppSetupKey)
        val navigator = WalletNavigator(backStack)

        navigator.navigate(DashboardKey)

        assertEquals(2, backStack.size)
        assertEquals(DashboardKey, backStack.last())
    }

    @Test
    fun `goBack removes last key from backstack`() {
        val backStack = NavBackStack<NavKey>(AppSetupKey, DashboardKey)
        val navigator = WalletNavigator(backStack)

        navigator.goBack()

        assertEquals(1, backStack.size)
        assertEquals(AppSetupKey, backStack.last())
    }

    @Test
    fun `goBack on empty backstack does not throw`() {
        val backStack = NavBackStack<NavKey>()
        val navigator = WalletNavigator(backStack)

        navigator.goBack()

        assertTrue(backStack.isEmpty())
    }

    @Test
    fun `resetToOnboarding clears backstack and adds IntroKey`() {
        val backStack = NavBackStack<NavKey>(AppSetupKey, DashboardKey, SettingsKey())
        val navigator = WalletNavigator(backStack)

        navigator.resetToOnboarding()

        assertEquals(1, backStack.size)
        assertEquals(IntroKey, backStack.single())
    }

    @Test
    fun `resetToDashboard clears backstack and adds DashboardKey`() {
        val backStack = NavBackStack<NavKey>(AppSetupKey, IntroKey, OnboardingKey)
        val navigator = WalletNavigator(backStack)

        navigator.resetToDashboard()

        assertEquals(1, backStack.size)
        assertEquals(DashboardKey, backStack.single())
    }
}
