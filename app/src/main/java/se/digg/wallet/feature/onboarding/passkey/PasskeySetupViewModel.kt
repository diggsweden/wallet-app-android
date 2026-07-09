// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.onboarding.passkey

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import se.digg.wallet.core.passkey.PasskeyCreateResult
import se.digg.wallet.core.passkey.PasskeyManager
import se.digg.wallet.core.services.KeystoreManager
import se.digg.wallet.data.UserRepository

sealed interface PasskeySetupUiState {
    data object Idle : PasskeySetupUiState
    data object Creating : PasskeySetupUiState
    data class Error(val message: String?) : PasskeySetupUiState
}

sealed interface PasskeySetupUiEvent {
    data object PasskeyCreated : PasskeySetupUiEvent
}

@HiltViewModel
class PasskeySetupViewModel @Inject constructor(
    private val passkeyManager: PasskeyManager,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<PasskeySetupUiState>(PasskeySetupUiState.Idle)
    val uiState: StateFlow<PasskeySetupUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<PasskeySetupUiEvent>()
    val events: SharedFlow<PasskeySetupUiEvent> = _events

    /**
     * [activityContext] is required by Credential Manager to show the system
     * passkey sheet; it is used for the single call and not retained.
     */
    fun createPasskey(activityContext: Context, pin: String) {
        if (_uiState.value == PasskeySetupUiState.Creating) return
        _uiState.value = PasskeySetupUiState.Creating
        viewModelScope.launch {
            when (val result = passkeyManager.createPasskey(activityContext, USER_NAME)) {
                is PasskeyCreateResult.Success -> {
                    userRepository.setPasskey(result.passkey)
                    userRepository.setEncryptedPin(KeystoreManager.encryptPin(pin))
                    _uiState.value = PasskeySetupUiState.Idle
                    _events.emit(PasskeySetupUiEvent.PasskeyCreated)
                }

                PasskeyCreateResult.Cancelled -> {
                    _uiState.value = PasskeySetupUiState.Idle
                }

                is PasskeyCreateResult.Failed -> {
                    _uiState.value = PasskeySetupUiState.Error(result.message)
                }
            }
        }
    }

    private companion object {
        const val USER_NAME = "Digg Wallet"
    }
}
