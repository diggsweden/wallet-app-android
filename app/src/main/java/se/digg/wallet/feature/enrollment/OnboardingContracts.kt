package se.digg.wallet.feature.enrollment

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.INTRO,
    val data: OnboardingData = OnboardingData(),
    val isLoading: Boolean = false,
    val error: String? = null

)

enum class OnboardingStep { INTRO, /*TERMS,*/ CONTACT, PIN, WUA, DONE }

data class OnboardingData(
    val acceptedTerms: Boolean = false,
    val email: String = "",
    val phone: String = "",
    val pin: String = ""
)

sealed interface OnboardingEvent {
    data object TermsCompleted : OnboardingEvent
    data object ContactInfoCompleted : OnboardingEvent
    data object Continue : OnboardingEvent
    data object Back : OnboardingEvent
    data object Finish : OnboardingEvent
}

sealed interface OnboardingEffect {
    data object NavigateToDashboard : OnboardingEffect
    data class Navigate(val route: String) : OnboardingEffect
}