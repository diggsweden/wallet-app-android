// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.onboarding

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
class OnboardingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val userRepository = mockk<UserRepository>(relaxed = true)

    private fun viewModel() = OnboardingViewModel(userRepository)

    @Test
    fun `starts on the first step`() {
        val state = viewModel().uiState.value

        assertEquals(OnboardingStep.SETUP_PIN, state.currentStep)
        assertEquals(OnboardingStep.entries.size, state.totalSteps)
        assertEquals("", state.capturedPin)
        assertEquals(listOf(OnboardingStep.VERIFY_PIN), state.enableBack)
    }

    @Test
    fun `Next advances one step`() {
        val vm = viewModel()

        vm.onAction(OnboardingAction.Next(OnboardingStep.SETUP_PIN))

        assertEquals(OnboardingStep.VERIFY_PIN, vm.uiState.value.currentStep)
    }

    @Test
    fun `Next from a step that is not current is ignored`() {
        val vm = viewModel()

        vm.onAction(OnboardingAction.Next(OnboardingStep.SETUP_PID))

        assertEquals(OnboardingStep.SETUP_PIN, vm.uiState.value.currentStep)
    }

    @Test
    fun `Next on the last step does not run past the end`() {
        val vm = viewModel()
        OnboardingStep.entries.dropLast(1).forEach { vm.onAction(OnboardingAction.Next(it)) }
        assertEquals(OnboardingStep.CREDENTIAL_OFFER, vm.uiState.value.currentStep)

        vm.onAction(OnboardingAction.Next(OnboardingStep.CREDENTIAL_OFFER))

        assertEquals(OnboardingStep.CREDENTIAL_OFFER, vm.uiState.value.currentStep)
    }

    @Test
    fun `Back returns to the previous step`() {
        val vm = viewModel()
        vm.onAction(OnboardingAction.Next(OnboardingStep.SETUP_PIN))

        vm.onAction(OnboardingAction.Back(OnboardingStep.VERIFY_PIN))

        assertEquals(OnboardingStep.SETUP_PIN, vm.uiState.value.currentStep)
    }

    @Test
    fun `Back on the first step stays put`() {
        val vm = viewModel()

        vm.onAction(OnboardingAction.Back(OnboardingStep.SETUP_PIN))

        assertEquals(OnboardingStep.SETUP_PIN, vm.uiState.value.currentStep)
    }

    @Test
    fun `Back from a step that is not current is ignored`() {
        val vm = viewModel()

        vm.onAction(OnboardingAction.Back(OnboardingStep.SETUP_PID))

        assertEquals(OnboardingStep.SETUP_PIN, vm.uiState.value.currentStep)
    }

    @Test
    fun `Skip jumps two steps ahead`() {
        val vm = viewModel()

        vm.onAction(OnboardingAction.Skip)

        assertEquals(OnboardingStep.SETUP_WALLET, vm.uiState.value.currentStep)
    }

    @Test
    fun `Skip is ignored on the last step`() {
        val vm = viewModel()
        OnboardingStep.entries.dropLast(1).forEach { vm.onAction(OnboardingAction.Next(it)) }

        vm.onAction(OnboardingAction.Skip)

        assertEquals(OnboardingStep.CREDENTIAL_OFFER, vm.uiState.value.currentStep)
    }

    @Test
    fun `Finish is handled by the UI layer and leaves state untouched`() {
        val vm = viewModel()

        vm.onAction(OnboardingAction.Finish)

        assertEquals(OnboardingStep.SETUP_PIN, vm.uiState.value.currentStep)
    }

    @Test
    fun `PinEntered captures the pin and advances`() {
        val vm = viewModel()

        vm.onAction(OnboardingAction.PinEntered(pin = "1234", fromStep = OnboardingStep.SETUP_PIN))

        assertEquals("1234", vm.uiState.value.capturedPin)
        assertEquals(OnboardingStep.VERIFY_PIN, vm.uiState.value.currentStep)
    }

    @Test
    fun `PinEntered from a stale step is ignored`() {
        val vm = viewModel()

        vm.onAction(OnboardingAction.PinEntered(pin = "1234", fromStep = OnboardingStep.VERIFY_PIN))

        assertEquals("", vm.uiState.value.capturedPin)
        assertEquals(OnboardingStep.SETUP_PIN, vm.uiState.value.currentStep)
    }

    @Test
    fun `a matching PinVerified advances past confirmation`() {
        val vm = viewModel()
        vm.onAction(OnboardingAction.PinEntered("1234", OnboardingStep.SETUP_PIN))

        vm.onAction(OnboardingAction.PinVerified("1234", OnboardingStep.VERIFY_PIN))

        assertEquals(OnboardingStep.SETUP_WALLET, vm.uiState.value.currentStep)
    }

    @Test
    fun `a mismatching PinVerified sends the user back to re-enter the pin`() {
        val vm = viewModel()
        vm.onAction(OnboardingAction.PinEntered("1234", OnboardingStep.SETUP_PIN))

        vm.onAction(OnboardingAction.PinVerified("9999", OnboardingStep.VERIFY_PIN))

        assertEquals(OnboardingStep.SETUP_PIN, vm.uiState.value.currentStep)
    }

    @Test
    fun `CredentialOfferFetched stores the offer and advances`() {
        val vm = viewModel()
        OnboardingStep.entries.dropLast(2).forEach {
            vm.onAction(OnboardingAction.Next(it))
        }
        assertEquals(OnboardingStep.SETUP_PID, vm.uiState.value.currentStep)

        vm.onAction(
            OnboardingAction.CredentialOfferFetched(
                url = "openid-credential-offer://offer",
                fromStep = OnboardingStep.SETUP_PID,
            ),
        )

        assertEquals("openid-credential-offer://offer", vm.getCredentialOfferUrl())
        assertEquals(OnboardingStep.CREDENTIAL_OFFER, vm.uiState.value.currentStep)
    }

    @Test
    fun `CredentialOfferFetched from a stale step is ignored`() {
        val vm = viewModel()

        vm.onAction(OnboardingAction.CredentialOfferFetched("offer", OnboardingStep.SETUP_PID))

        assertEquals("", vm.getCredentialOfferUrl())
    }

    @Test
    fun `Close wipes local storage and reports it`() = runTest {
        coEvery { userRepository.wipeAll() } returns Unit
        val vm = viewModel()

        val events = mutableListOf<OnboardingUiEvent>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.events.collect { events += it }
        }

        vm.onAction(OnboardingAction.Close)

        assertEquals(listOf(OnboardingUiEvent.LocalStorageCleared), events)
        coVerify(exactly = 1) { userRepository.wipeAll() }
        collector.cancel()
    }
}
