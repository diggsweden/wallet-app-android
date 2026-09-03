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

    private fun navigator(vararg keys: NavKey) = WalletNavigator(NavBackStack(*keys))

    @Test
    fun `navigate pushes onto the back stack`() {
        val navigator = navigator(IntroKey)

        navigator.navigate(OnboardingKey)
        navigator.navigate(CredentialDetailsKey("cred-1"))

        assertEquals(
            listOf(IntroKey, OnboardingKey, CredentialDetailsKey("cred-1")),
            navigator.backStack.toList(),
        )
    }

    @Test
    fun `goBack pops the top entry`() {
        val navigator = navigator(IntroKey, OnboardingKey)

        navigator.goBack()

        assertEquals(listOf(IntroKey), navigator.backStack.toList())
    }

    @Test
    fun `goBack on an empty stack is a no-op`() {
        val navigator = navigator()

        navigator.goBack()

        assertTrue(navigator.backStack.isEmpty())
    }

    @Test
    fun `resetToOnboarding replaces the stack with the intro screen`() {
        val navigator = navigator(DashboardKey, SettingsKey(), CredentialDetailsKey("c"))

        navigator.resetToOnboarding()

        assertEquals(listOf<NavKey>(IntroKey), navigator.backStack.toList())
    }

    @Test
    fun `resetToDashboard replaces the stack with the dashboard`() {
        val navigator = navigator(IntroKey, OnboardingKey)

        navigator.resetToDashboard()

        assertEquals(listOf<NavKey>(DashboardKey), navigator.backStack.toList())
    }

    @Test
    fun `resetting from an empty stack still lands on one entry`() {
        val navigator = navigator()

        navigator.resetToDashboard()

        assertEquals(listOf<NavKey>(DashboardKey), navigator.backStack.toList())
    }
}
