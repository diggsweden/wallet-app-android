package se.digg.wallet.feature.enrollment.credentialoffer

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class CredentialOfferViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow<CredentialOfferUiState>(CredentialOfferUiState.Loading)
    val uiState: StateFlow<CredentialOfferUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<CredentialOfferUiEffect>()
    val effects: SharedFlow<CredentialOfferUiEffect> = _effects.asSharedFlow()
}
