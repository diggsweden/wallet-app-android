package se.digg.wallet.feature.enrollment.pin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import se.digg.wallet.data.UserRepository
import javax.inject.Inject

@HiltViewModel
class PinSetupViewModel @Inject constructor(private val userRepository: UserRepository) :
    ViewModel() {

    private val _uiState = MutableStateFlow(PinSetupUiState())
    val uiState: StateFlow<PinSetupUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<PinSetupUiEffect>()
    val effects: SharedFlow<PinSetupUiEffect> = _effects.asSharedFlow()

    fun setPin(pinCode: String) {
        viewModelScope.launch {
            userRepository.setPin(pinCode)
            _effects.emit(PinSetupUiEffect.OnNext)

        }
    }

    fun checkIfValid(isVerifyScreen: Boolean, code: String) {
        viewModelScope.launch {
            val storedCode = userRepository.getPin()
            storedCode?.let {
                if (storedCode == code) {
                    _effects.emit(PinSetupUiEffect.OnNext)
                } else {
                    _effects.emit(PinSetupUiEffect.OnGoBack)
                }
            }
        }
    }
}