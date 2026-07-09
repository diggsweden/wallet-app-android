// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.onboarding

data class OnboardingUiState(
    val currentStep: OnboardingStep = OnboardingStep.SETUP_PIN,
    val totalSteps: Int = OnboardingStep.totalSteps,
    val enableBack: List<OnboardingStep> =
        listOf(
            OnboardingStep.SETUP_PASSKEY,
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
    data class PasskeyCreated(val fromStep: OnboardingStep) : OnboardingAction
}

enum class OnboardingStep {
    SETUP_PIN,

    // Passkey PoC: replaces the previous VERIFY_PIN step to show where
    // passkey creation would sit in a real implementation.
    SETUP_PASSKEY,
    SETUP_WALLET,
    SETUP_PID,
    CREDENTIAL_OFFER,
    ;

    companion object {
        val totalSteps: Int get() = entries.size
    }
}
