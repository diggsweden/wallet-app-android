package se.digg.wallet

import android.util.Log
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
import se.digg.wallet.feature.enrollment.consent.ConsentUiEffect
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor() :
    ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _effects = MutableSharedFlow<AuthEffect>()
    val effects: SharedFlow<AuthEffect> = _effects.asSharedFlow()


    init {
        Log.d("AuthViewModel", "Instance: ${hashCode()}")
    }

    fun startAuthTab(uri: String) {
        viewModelScope.launch {
            _effects.emit(
                AuthEffect.LaunchAuthTab(
                    uri
                )
            )
        }
    }

}

sealed interface AuthEffect {
    data class LaunchAuthTab(val uri: String) : AuthEffect
}

sealed interface AuthState {
    object Idle : AuthState
    object Loading : AuthState
    object Error : AuthState
}

