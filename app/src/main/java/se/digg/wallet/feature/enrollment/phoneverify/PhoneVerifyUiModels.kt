package se.digg.wallet.feature.enrollment.phoneverify

data class PhoneVerifyUiState(
    val phone: String = "",
    val code: String = "",
    val showError: Boolean = false
)

sealed interface PhoneVerifyUiEvent {
    data class CodeChanged(val value: String) : PhoneVerifyUiEvent
    object NextClicked : PhoneVerifyUiEvent
}

sealed interface PhoneVerifyUiEffect {
    object OnNext : PhoneVerifyUiEffect
}