package se.digg.wallet.feature.enrollment.email

data class EmailUiState(
    val email: String = "",
    val verifyEmail: String = "",
    val emailError: EmailValidationError? = null,
    val verifyEmailError: EmailValidationError? = null,
)

sealed interface EmailUiEvent {
    data class EmailChanged(val value: String) : EmailUiEvent
    data class VerifyEmailChanged(val value: String) : EmailUiEvent
    data class EmailFocusedChanged(val isFocused: Boolean) : EmailUiEvent
    data class VerifyEmailFocusedChanged(val isFocused: Boolean) : EmailUiEvent
    object NextClicked : EmailUiEvent
}

sealed interface EmailUiEffect {
    object OnNext : EmailUiEffect
}

enum class EmailValidationError {
    EMPTY,
    NOT_VALID_EMAIL,
    NOT_SAME,
}
