package se.digg.wallet.feature.enrollment.fetchid

sealed interface FetchIdUiState {
    object Idle : FetchIdUiState
    object Loading : FetchIdUiState
    object Error : FetchIdUiState
}

sealed interface FetchIdUiEvent {
    object NextClicked : FetchIdUiEvent
    object FetchIdClicked : FetchIdUiEvent
}

sealed interface FetchIdUiEffect {
    object OnNext : FetchIdUiEffect
}
