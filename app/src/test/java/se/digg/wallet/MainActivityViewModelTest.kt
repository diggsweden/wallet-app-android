// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import se.digg.wallet.data.UserRepository
import se.digg.wallet.util.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class MainActivityViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val userRepository = mockk<UserRepository>(relaxed = true)

    @Test
    fun `AppFlowState defaults to onboarding with no dashboard route`() {
        val state = AppFlowState()

        assertEquals(AppFlow.Onboarding, state.flow)
        assertNull(state.dashboardStartRoute)
    }

    @Test
    fun `an onboarded user goes straight to the dashboard`() {
        coEvery { userRepository.isOnboarded() } returns true

        val vm = MainActivityViewModel(userRepository)

        assertEquals(AppFlow.Dashboard, vm.onboardingState.value.flow)
        coVerify(exactly = 0) { userRepository.wipeAll() }
    }

    @Test
    fun `a half-finished setup is wiped before onboarding restarts`() {
        coEvery { userRepository.isOnboarded() } returns false

        val vm = MainActivityViewModel(userRepository)

        assertEquals(AppFlow.Onboarding, vm.onboardingState.value.flow)
        coVerify(exactly = 1) { userRepository.wipeAll() }
    }

    @Test
    fun `goToDashboard carries a start route`() {
        coEvery { userRepository.isOnboarded() } returns false
        val vm = MainActivityViewModel(userRepository)

        vm.goToDashboard(startRoute = "credential/1")

        assertEquals(AppFlow.Dashboard, vm.onboardingState.value.flow)
        assertEquals("credential/1", vm.onboardingState.value.dashboardStartRoute)
    }

    @Test
    fun `goToOnboarding clears any pending dashboard route`() {
        coEvery { userRepository.isOnboarded() } returns true
        val vm = MainActivityViewModel(userRepository)
        vm.goToDashboard(startRoute = "credential/1")

        vm.goToOnboarding()

        assertEquals(AppFlow.Onboarding, vm.onboardingState.value.flow)
        assertNull(vm.onboardingState.value.dashboardStartRoute)
    }
}
