// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.settings

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import se.digg.wallet.data.UserRepository
import se.digg.wallet.util.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val userRepository = mockk<UserRepository>(relaxed = true)

    @Test
    fun `logout wipes local storage and reports it`() = runTest {
        coEvery { userRepository.wipeAll() } returns Unit
        val vm = SettingsViewModel(userRepository)
        val events = mutableListOf<SettingsUiEvent>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.events.collect { events += it }
        }

        vm.onLogout()

        assertEquals(listOf(SettingsUiEvent.LocalStorageCleared), events)
        coVerify(exactly = 1) { userRepository.wipeAll() }
        collector.cancel()
    }

    @Test
    fun `logout reports once per invocation`() = runTest {
        coEvery { userRepository.wipeAll() } returns Unit
        val vm = SettingsViewModel(userRepository)
        val events = mutableListOf<SettingsUiEvent>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.events.collect { events += it }
        }

        vm.onLogout()
        vm.onLogout()

        assertEquals(2, events.size)
        coVerify(exactly = 2) { userRepository.wipeAll() }
        collector.cancel()
    }
}
