package se.digg.wallet.feature.enrollment.emailverify

data class EmailVerifyUiState(
    val code: String = "",
    val showError: Boolean = false,
    val email: String = "",
)

sealed interface EmailVerifyUiEvent {
    data class CodeChanged(val value: String) : EmailVerifyUiEvent
    object NextClicked : EmailVerifyUiEvent
}

sealed interface EmailVerifyUiEffect {
    object OnNext : EmailVerifyUiEffect
}
