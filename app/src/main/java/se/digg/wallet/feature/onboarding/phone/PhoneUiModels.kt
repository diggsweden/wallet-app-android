package se.digg.wallet.feature.onboarding.phone

data class PhoneUiState(val phone: String = "", val showError: Boolean = false)

sealed interface PhoneUiEvent {
    data class PhoneChanged(val value: String) : PhoneUiEvent
    data class PhoneFocusedChanged(val isFocused: Boolean) : PhoneUiEvent
    object NextClicked : PhoneUiEvent
    object SkipClicked : PhoneUiEvent
}

sealed interface PhoneUiEffect {
    object OnNext : PhoneUiEffect
    object OnSkip : PhoneUiEffect
}
