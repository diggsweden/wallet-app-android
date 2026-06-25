// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.onboarding

data class OnboardingUiState(
    val currentStep: OnboardingStep = OnboardingStep.SETUP_PIN,
    val totalSteps: Int = OnboardingStep.totalSteps,
    val enableBack: List<OnboardingStep> =
        listOf(
            OnboardingStep.VERIFY_PIN,
        ),
    val capturedPin: String = "",
)

sealed interface OnboardingUiEvent {
    data object LocalStorageCleared : OnboardingUiEvent
}

sealed interface OnboardingAction {
    data class Next(val fromStep: OnboardingStep) : OnboardingAction
    data class Back(val fromStep: OnboardingStep) : OnboardingAction
    data object Skip : OnboardingAction
    data object Finish : OnboardingAction
    data object Close : OnboardingAction
    data class CredentialOfferFetched(val url: String, val fromStep: OnboardingStep) :
        OnboardingAction

    data class PinEntered(val pin: String, val fromStep: OnboardingStep) : OnboardingAction
    data class PinVerified(val pin: String, val fromStep: OnboardingStep) : OnboardingAction
}

enum class OnboardingStep {
    SETUP_PIN,
    VERIFY_PIN,
    SETUP_WALLET,
    SETUP_PID,
    CREDENTIAL_OFFER,
    ;

    companion object {
        val totalSteps: Int get() = entries.size
    }
}
