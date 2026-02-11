package se.digg.wallet.feature.presentation

import se.digg.wallet.data.DisclosureLocal

sealed interface PresentationUiState {
    object Initial : PresentationUiState
    object Loading : PresentationUiState
    data class SelectDisclosures(val disclosures: List<DisclosureLocal>) : PresentationUiState
    object ShareSuccess : PresentationUiState
    data class Error(val errorMessage: String?) : PresentationUiState
}

sealed interface PresentationUiEffect {
    data class OpenUrl(val url: String) : PresentationUiEffect
}
