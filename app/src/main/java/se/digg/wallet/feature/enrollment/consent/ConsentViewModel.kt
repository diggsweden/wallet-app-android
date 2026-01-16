package se.digg.wallet.feature.enrollment.consent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import se.digg.wallet.data.UserRepository
import javax.inject.Inject

@HiltViewModel
class ConsentViewModel @Inject constructor(private val userRepository: UserRepository) :
    ViewModel() {

    private val _uiState = MutableStateFlow(ConsentUiState())
    val uiState: StateFlow<ConsentUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<ConsentUiEffect>()
    val effects: SharedFlow<ConsentUiEffect> = _effects.asSharedFlow()

    fun onEvent(event: ConsentUiEvent) {
        when (event) {
            is ConsentUiEvent.ConsentChanged -> setConsent(event.isChecked)
            is ConsentUiEvent.NextClicked -> validateAndNext()
        }
    }

    private fun validateAndNext() {
        val state = _uiState.value
        if (state.hasConsent) {
            viewModelScope.launch {
                _effects.emit(ConsentUiEffect.OnNext)
            }
        } else {
            _uiState.update { it.copy(showError = true) }
        }
    }

    private fun setConsent(isChecked: Boolean) {
        _uiState.update { it.copy(hasConsent = isChecked) }
        if (isChecked) {
            _uiState.update { it.copy(showError = false) }
        }
    }
}