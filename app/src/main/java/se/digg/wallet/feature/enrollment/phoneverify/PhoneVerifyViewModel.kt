// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.enrollment.phoneverify

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import se.digg.wallet.data.UserRepository

@HiltViewModel
class PhoneVerifyViewModel @Inject constructor(private val userRepository: UserRepository) :
    ViewModel() {

    private val _uiState = MutableStateFlow(PhoneVerifyUiState())
    val uiState: StateFlow<PhoneVerifyUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<PhoneVerifyUiEffect>()
    val effects: SharedFlow<PhoneVerifyUiEffect> = _effects.asSharedFlow()

    fun onEvent(event: PhoneVerifyUiEvent) {
        when (event) {
            is PhoneVerifyUiEvent.CodeChanged -> updateCode(event.value)
            PhoneVerifyUiEvent.NextClicked -> onNextClicked()
        }
    }

    private fun updateCode(value: String) {
        _uiState.update { it.copy(code = value) }
    }

    fun setPhoneNumber() {
        viewModelScope.launch {
            _uiState.update { it.copy(phone = userRepository.getPhone() ?: "-") }
        }
    }

    private fun onNextClicked() {
        val code = _uiState.value.code
        if (code.length == 6) {
            viewModelScope.launch {
                _effects.emit(PhoneVerifyUiEffect.OnNext)
            }
        }
    }
}
