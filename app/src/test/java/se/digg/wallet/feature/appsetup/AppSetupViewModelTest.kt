// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.appsetup

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
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
import se.digg.wallet.core.trust.TrustStore

private class FakeAppSetupService : AppSetupService {
    var destination = StartDestination.Intro
    var resolveError: Exception? = null
    var clearError: Exception? = null
    var clearCount = 0

    override suspend fun resolveStartDestination(): StartDestination {
        resolveError?.let { throw it }
        return destination
    }

    override suspend fun clearUserData() {
        clearCount++
        clearError?.let { throw it }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AppSetupViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val service = FakeAppSetupService()
    private val offlineClient = HttpClient(
        MockEngine {
            respondError(HttpStatusCode.ServiceUnavailable)
        },
    )
    private val trustStore = TrustStore(
        gatewayHttpClient = offlineClient,
        baseHttpClient = offlineClient,
        applicationScope = CoroutineScope(Job()),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.collectEffects(viewModel: AppSetupViewModel): List<AppSetupUiEffect> {
        val effects = mutableListOf<AppSetupUiEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }
        return effects
    }

    @Test
    fun `the intro destination navigates to the intro`() = runTest(dispatcher) {
        service.destination = StartDestination.Intro
        val viewModel = AppSetupViewModel(service, trustStore)
        val effects = collectEffects(viewModel)

        advanceUntilIdle()

        assertEquals(listOf(AppSetupUiEffect.NavigateToIntro), effects)
    }

    @Test
    fun `the dashboard destination navigates to the dashboard without wiping user data`() =
        runTest(dispatcher) {
            service.destination = StartDestination.Dashboard
            val viewModel = AppSetupViewModel(service, trustStore)
            val effects = collectEffects(viewModel)

            advanceUntilIdle()

            assertEquals(listOf(AppSetupUiEffect.NavigateToDashboard), effects)
            assertEquals(0, service.clearCount)
        }

    @Test
    fun `the reset screen is shown once user data is wiped`() = runTest(dispatcher) {
        service.destination = StartDestination.Reset
        val viewModel = AppSetupViewModel(service, trustStore)
        val effects = collectEffects(viewModel)

        advanceUntilIdle()

        assertEquals(AppSetupUiState.Reset, viewModel.uiState.value)
        assertEquals(1, service.clearCount)
        assertTrue(effects.isEmpty())
    }

    @Test
    fun `acknowledging the reset navigates to the intro`() = runTest(dispatcher) {
        service.destination = StartDestination.Reset
        val viewModel = AppSetupViewModel(service, trustStore)
        val effects = collectEffects(viewModel)
        advanceUntilIdle()

        viewModel.onResetAcknowledged()
        advanceUntilIdle()

        assertEquals(listOf(AppSetupUiEffect.NavigateToIntro), effects)
    }

    @Test
    fun `a failed wipe shows the error instead of the reset screen`() = runTest(dispatcher) {
        service.destination = StartDestination.Reset
        service.clearError = IllegalStateException("wipe failed")
        val viewModel = AppSetupViewModel(service, trustStore)
        val states = mutableListOf<AppSetupUiState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { states.add(it) }
        }

        advanceUntilIdle()

        assertEquals(
            listOf(AppSetupUiState.Loading, AppSetupUiState.Error(message = "wipe failed")),
            states,
        )
    }

    @Test
    fun `a failed destination check shows the error without navigating`() = runTest(dispatcher) {
        service.resolveError = IllegalStateException("offline")
        val viewModel = AppSetupViewModel(service, trustStore)
        val effects = collectEffects(viewModel)

        advanceUntilIdle()

        assertEquals(AppSetupUiState.Error(message = "offline"), viewModel.uiState.value)
        assertTrue(effects.isEmpty())
    }

    @Test
    fun `retrying a failed destination check shows loading again`() = runTest(dispatcher) {
        service.resolveError = IllegalStateException("offline")
        val viewModel = AppSetupViewModel(service, trustStore)
        advanceUntilIdle()

        viewModel.onRetry()

        assertEquals(AppSetupUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `a successful retry navigates to the resolved destination`() = runTest(dispatcher) {
        service.resolveError = IllegalStateException("offline")
        val viewModel = AppSetupViewModel(service, trustStore)
        val effects = collectEffects(viewModel)
        advanceUntilIdle()
        service.resolveError = null
        service.destination = StartDestination.Dashboard

        viewModel.onRetry()
        advanceUntilIdle()

        assertEquals(listOf(AppSetupUiEffect.NavigateToDashboard), effects)
    }
}
