// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.onboarding.walletsetup

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import se.digg.wallet.core.error.AppError
import se.digg.wallet.core.error.AppException
import se.digg.wallet.util.MainDispatcherRule

/** A service that fails a chosen step with a chosen exception, then stops failing. */
private class ScriptedWalletSetupService : WalletSetupService {
    var failAt: SetupStep? = null
    var failWith: () -> Throwable = { IllegalStateException("boom") }
    val calls = mutableListOf<SetupStep>()

    private fun record(step: SetupStep) {
        calls += step
        if (step == failAt) throw failWith()
    }

    override suspend fun createAccount() = record(SetupStep.CREATE_ACCOUNT)
    override suspend fun initHsm() = record(SetupStep.INIT_HSM)
    override suspend fun registerPin(pin: String) = record(SetupStep.REGISTER_PIN)
    override suspend fun authenticate(pin: String) = record(SetupStep.AUTHENTICATE)
    override suspend fun postHsmKey() = record(SetupStep.POST_HSM_KEY)
}

@OptIn(ExperimentalCoroutinesApi::class)
class WalletSetupRetryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val service = ScriptedWalletSetupService()

    // AppError.Problem timestamps itself at construction, so the test holds one instance.
    private val problem = AppError.Problem(
        status = 409,
        title = "Account exists",
        detail = "An account is already registered for this device",
        type = "urn:problem:conflict",
        instance = null,
        transactionId = "tx-1",
    )

    @Test
    fun `a structured problem is surfaced with its title and detail`() = runTest {
        service.failAt = SetupStep.INIT_HSM
        service.failWith = { AppException(problem) }
        val vm = WalletSetupViewModel(service)

        vm.start("1234")
        advanceUntilIdle()

        val failed = vm.uiState.value as WalletSetupUiState.Failed
        assertEquals(SetupStep.INIT_HSM, failed.step)
        assertEquals("Account exists", failed.error.title)
        assertEquals("An account is already registered for this device", failed.error.message)
        assertEquals(problem, failed.error.problem)
    }

    @Test
    fun `an unstructured AppError is surfaced without details`() = runTest {
        service.failAt = SetupStep.REGISTER_PIN
        service.failWith = { AppException(AppError.Connectivity(null)) }
        val vm = WalletSetupViewModel(service)

        vm.start("1234")
        advanceUntilIdle()

        val failed = vm.uiState.value as WalletSetupUiState.Failed
        assertEquals(SetupStep.REGISTER_PIN, failed.step)
        assertNull(failed.error.title)
        assertNull(failed.error.problem)
    }

    @Test
    fun `retry resumes from the failed step without repeating earlier ones`() = runTest {
        service.failAt = SetupStep.REGISTER_PIN
        val vm = WalletSetupViewModel(service)
        vm.start("1234")
        advanceUntilIdle()
        assertTrue(vm.uiState.value is WalletSetupUiState.Failed)
        service.calls.clear()
        service.failAt = null

        vm.retry()
        advanceUntilIdle()

        assertEquals(
            listOf(SetupStep.REGISTER_PIN, SetupStep.AUTHENTICATE, SetupStep.POST_HSM_KEY),
            service.calls,
        )
    }

    @Test
    fun `retry is ignored while setup is still in progress`() = runTest {
        val vm = WalletSetupViewModel(service)

        vm.retry()
        advanceUntilIdle()

        assertEquals(emptyList<SetupStep>(), service.calls)
        assertEquals(WalletSetupUiState.InProgress(SetupStep.CREATE_ACCOUNT), vm.uiState.value)
    }
}
