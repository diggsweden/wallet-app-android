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
)

sealed interface OnboardingUiEvent {
    data object LocalStorageCleared : OnboardingUiEvent
}

sealed interface OnboardingUiEffect {
    object OnNext : OnboardingUiEffect
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
