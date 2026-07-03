// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.onboarding.walletsetup

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakeWalletSetupService : WalletSetupService {
    var failAt: SetupStep? = null
    val callCounts = mutableMapOf<SetupStep, Int>().withDefault { 0 }

    private fun record(step: SetupStep) {
        callCounts[step] = callCounts.getValue(step) + 1
        if (step == failAt) {
            throw IllegalStateException("Intentional failure at $step")
        }
    }

    override suspend fun createAccount() = record(SetupStep.CREATE_ACCOUNT)

    override suspend fun initHsm() = record(SetupStep.INIT_HSM)

    override suspend fun registerPin(pin: String) = record(SetupStep.REGISTER_PIN)

    override suspend fun authenticate(pin: String) = record(SetupStep.AUTHENTICATE)

    override suspend fun postHsmKey() = record(SetupStep.POST_HSM_KEY)
}

@OptIn(ExperimentalCoroutinesApi::class)
class WalletSetupViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val service = FakeWalletSetupService()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.collectEffects(
        viewModel: WalletSetupViewModel,
    ): List<WalletSetupUiEffect> {
        val effects = mutableListOf<WalletSetupUiEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }
        return effects
    }

    @Test
    fun `runs all steps once and emits OnNext on success`() = runTest(dispatcher) {
        val viewModel = WalletSetupViewModel(service)
        val effects = collectEffects(viewModel)

        viewModel.start(pin = "123456")
        advanceUntilIdle()

        assertEquals(listOf(WalletSetupUiEffect.OnNext), effects)
        SetupStep.entries.forEach { step ->
            assertEquals(1, service.callCounts.getValue(step))
        }
    }

    @Test
    fun `failure sets failed state for the failing step`() = runTest(dispatcher) {
        service.failAt = SetupStep.AUTHENTICATE
        val viewModel = WalletSetupViewModel(service)
        val effects = collectEffects(viewModel)

        viewModel.start(pin = "123456")
        advanceUntilIdle()

        assertEquals(WalletSetupUiState.Failed(SetupStep.AUTHENTICATE), viewModel.uiState.value)
        assertTrue(effects.isEmpty())
        assertEquals(0, service.callCounts.getValue(SetupStep.POST_HSM_KEY))
    }

    @Test
    fun `retry resumes from the failed step`() = runTest(dispatcher) {
        service.failAt = SetupStep.AUTHENTICATE
        val viewModel = WalletSetupViewModel(service)
        val effects = collectEffects(viewModel)

        viewModel.start(pin = "123456")
        advanceUntilIdle()

        service.failAt = null
        viewModel.retry()
        advanceUntilIdle()

        assertEquals(listOf(WalletSetupUiEffect.OnNext), effects)
        assertEquals(1, service.callCounts.getValue(SetupStep.CREATE_ACCOUNT))
        assertEquals(1, service.callCounts.getValue(SetupStep.INIT_HSM))
        assertEquals(1, service.callCounts.getValue(SetupStep.REGISTER_PIN))
        assertEquals(2, service.callCounts.getValue(SetupStep.AUTHENTICATE))
        assertEquals(1, service.callCounts.getValue(SetupStep.POST_HSM_KEY))
    }

    @Test
    fun `retry after post key failure retries only the post step`() = runTest(dispatcher) {
        service.failAt = SetupStep.POST_HSM_KEY
        val viewModel = WalletSetupViewModel(service)
        val effects = collectEffects(viewModel)

        viewModel.start(pin = "123456")
        advanceUntilIdle()

        service.failAt = null
        viewModel.retry()
        advanceUntilIdle()

        assertEquals(listOf(WalletSetupUiEffect.OnNext), effects)
        assertEquals(1, service.callCounts.getValue(SetupStep.AUTHENTICATE))
        assertEquals(2, service.callCounts.getValue(SetupStep.POST_HSM_KEY))
    }

    @Test
    fun `retry does nothing when setup has not failed`() = runTest(dispatcher) {
        val viewModel = WalletSetupViewModel(service)
        collectEffects(viewModel)

        viewModel.start(pin = "123456")
        advanceUntilIdle()

        viewModel.retry()
        advanceUntilIdle()

        SetupStep.entries.forEach { step ->
            assertEquals(1, service.callCounts.getValue(step))
        }
    }
}
