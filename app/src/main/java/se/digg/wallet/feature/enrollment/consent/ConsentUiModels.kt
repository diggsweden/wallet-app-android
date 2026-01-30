package se.digg.wallet.feature.enrollment.consent

data class ConsentUiState(val hasConsent: Boolean = false, val showError: Boolean = false)

sealed interface ConsentUiEvent {
    data class ConsentChanged(val isChecked: Boolean) : ConsentUiEvent
    object NextClicked : ConsentUiEvent
}

sealed interface ConsentUiEffect {
    object OnNext : ConsentUiEffect
}
