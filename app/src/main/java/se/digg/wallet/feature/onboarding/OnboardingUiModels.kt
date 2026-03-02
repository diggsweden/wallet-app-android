package se.digg.wallet.feature.onboarding

data class OnboardingUiState(
    val currentStep: OnboardingStep = OnboardingStep.NOTIFICATION,
    val totalSteps: Int = OnboardingStep.totalSteps,
    val enableBack: List<OnboardingStep> =
        listOf(
            OnboardingStep.VERIFY_EMAIL,
            OnboardingStep.VERIFY_PHONE,
            OnboardingStep.VERIFY_PIN,
            OnboardingStep.CREDENTIAL_OFFER,
        ),
)

sealed interface OnboardingUiEvent {
    data object LocalStorageCleared : OnboardingUiEvent
}

sealed interface OnboardingUiEffect {
    object OnNext : OnboardingUiEffect
}

enum class OnboardingStep(val stepTitle: String) {
    NOTIFICATION("Notification"),
    PHONE_NUMBER("Phone number"),
    VERIFY_PHONE("Verify phone"),
    EMAIL("Email"),
    VERIFY_EMAIL("Verify email"),
    PIN("PIN"),
    VERIFY_PIN("Verify PIN"),
    FETCH_PID("PID"),
    CREDENTIAL_OFFER("Credential offer"),
    ;

    companion object {
        val totalSteps: Int get() = entries.size
    }
}
