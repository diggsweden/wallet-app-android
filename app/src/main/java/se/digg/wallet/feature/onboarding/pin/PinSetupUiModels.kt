package se.digg.wallet.feature.onboarding.pin

data class PinSetupUiState(val pin: String = "")

sealed interface PinSetupUiEvent {
    object NextClicked : PinSetupUiEvent
}

sealed interface PinSetupUiEffect {
    object OnNext : PinSetupUiEffect
    object OnGoBack : PinSetupUiEffect
}
