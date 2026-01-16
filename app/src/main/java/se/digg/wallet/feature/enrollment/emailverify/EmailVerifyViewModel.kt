package se.digg.wallet.feature.enrollment.emailverify

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
class EmailVerifyViewModel @Inject constructor(private val userRepository: UserRepository) :
    ViewModel() {

    private val _uiState = MutableStateFlow(EmailVerifyUiState())
    val uiState: StateFlow<EmailVerifyUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<EmailVerifyUiEffect>()
    val effects: SharedFlow<EmailVerifyUiEffect> = _effects.asSharedFlow()

    fun onEvent(event: EmailVerifyUiEvent) {
        when (event) {
            is EmailVerifyUiEvent.CodeChanged -> updateCode(event.value)
            EmailVerifyUiEvent.NextClicked -> onNextClicked()
        }
    }

    fun setEmail() {
        viewModelScope.launch {
            _uiState.update { it.copy(email = userRepository.getEmail() ?: "-") }
        }
    }

    private fun updateCode(value: String) {
        _uiState.update { it.copy(code = value) }
    }

    private fun onNextClicked() {
        val code = _uiState.value.code
        if (code.length == 6) {
            viewModelScope.launch {
                _effects.emit(EmailVerifyUiEffect.OnNext)
            }
        }
    }
}