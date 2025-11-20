package se.digg.wallet.feature.enrollment.pin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import se.digg.wallet.data.UserRepository
import javax.inject.Inject

sealed interface PinState {
    object Pin : PinState
    object VerifyPin : PinState
}

sealed interface UiEffect {
    object Verified : UiEffect
}

@HiltViewModel
class PinViewModel@Inject constructor(private val userRepository: UserRepository) :
    ViewModel() {

    val _uiState = MutableStateFlow<PinState>(PinState.Pin)
    val uiState: StateFlow<PinState> = _uiState

    private val _effects = MutableSharedFlow<UiEffect>()
    val effects: SharedFlow<UiEffect> = _effects.asSharedFlow()

    var pin: String = ""

    fun setPin(input: String) = viewModelScope.launch { userRepository.setPin(input) }

    fun onPinSubmit(input: String) {
        when (uiState.value) {
            PinState.Pin -> {
                pin = input
                _uiState.value = PinState.VerifyPin
            }

            PinState.VerifyPin -> {
                if (pin.isNotEmpty() && input.isNotEmpty() && pin == input) {
                    setPin(input)
                    viewModelScope.launch {
                        _effects.emit(UiEffect.Verified)
                    }
                } else {
                    _uiState.value = PinState.Pin
                }
            }
        }
    }
}
