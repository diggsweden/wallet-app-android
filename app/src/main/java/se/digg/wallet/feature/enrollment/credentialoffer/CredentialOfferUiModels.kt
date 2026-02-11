package se.digg.wallet.feature.enrollment.credentialoffer

sealed interface CredentialOfferUiState {
    object Idle : CredentialOfferUiState
    object Loading : CredentialOfferUiState
    object Error : CredentialOfferUiState
}

sealed interface CredentialOfferUiEvent {
    object NextClicked : CredentialOfferUiEvent
    data class CredentialOffer(val credentialOffer: String)
}

sealed interface CredentialOfferUiEffect {
    object OnNext : CredentialOfferUiEffect
}
