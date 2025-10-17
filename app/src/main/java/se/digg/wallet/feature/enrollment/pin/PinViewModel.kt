package se.digg.wallet.feature.enrollment.pin

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import se.digg.wallet.core.storage.user.DatabaseProvider
import se.digg.wallet.core.storage.user.UserRepository

sealed interface PinState {
    object Pin : PinState
    object VerifyPin : PinState
}

sealed interface UiEffect {
    object Verified : UiEffect
}

class PinViewModel(private val repo: UserRepository) : ViewModel() {

    val _uiState = MutableStateFlow<PinState>(PinState.Pin)
    val uiState: StateFlow<PinState> = _uiState

    private val _effects = MutableSharedFlow<UiEffect>()
    val effects: SharedFlow<UiEffect> = _effects.asSharedFlow()

    var pin: String = ""

    fun setPin(input: String) = viewModelScope.launch { repo.setPin(input) }

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

    class Factory(private val appContext: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = DatabaseProvider.get(appContext)
            val repo = UserRepository(db.userDao())
            return PinViewModel(repo) as T
        }
    }
}
