// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.settings

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import se.digg.wallet.core.data.AccountManager

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()) :
    TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

private class FakeAccountManager : AccountManager {
    var wipeAllCallCount = 0

    override suspend fun wipeAll() {
        wipeAllCallCount++
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun onLogout_callsWipeAllAndEmitsLocalStorageCleared() = runTest {
        val accountManager = FakeAccountManager()
        val viewModel = SettingsViewModel(accountManager = accountManager)

        viewModel.events.test {
            viewModel.onLogout()

            val event = awaitItem()
            assertEquals(SettingsUiEvent.LocalStorageCleared, event)
            assertEquals(1, accountManager.wipeAllCallCount)
        }
    }

    @Test
    fun initialViewModel_emitsNoEvents() = runTest {
        val accountManager = FakeAccountManager()
        val viewModel = SettingsViewModel(accountManager = accountManager)

        viewModel.events.test {
            expectNoEvents()
            assertEquals(0, accountManager.wipeAllCallCount)
        }
    }
}
