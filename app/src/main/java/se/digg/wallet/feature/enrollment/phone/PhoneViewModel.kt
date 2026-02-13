// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.enrollment.phone

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
import timber.log.Timber

@HiltViewModel
class PhoneViewModel @Inject constructor(private val userRepository: UserRepository) :
    ViewModel() {

    private val _uiState = MutableStateFlow(PhoneUiState())
    val uiState: StateFlow<PhoneUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<PhoneUiEffect>()
    val effects: SharedFlow<PhoneUiEffect> = _effects.asSharedFlow()

    fun onEvent(event: PhoneUiEvent) {
        when (event) {
            is PhoneUiEvent.PhoneChanged -> {
                updatePhone(event.value)
            }

            is PhoneUiEvent.PhoneFocusedChanged -> {}

            PhoneUiEvent.NextClicked -> {
                validatePhoneNumber()
            }

            PhoneUiEvent.SkipClicked -> {
                onSkipClicked()
            }
        }
    }

    private fun onSkipClicked() {
        viewModelScope.launch {
            _effects.emit(PhoneUiEffect.OnSkip)
        }
    }

    private fun updatePhone(raw: String) {
        val value = raw.trim()
        _uiState.update { it.copy(phone = value) }
        val phone = _uiState.value.phone
        if (isValidPhoneNumber(phone)) {
            _uiState.update { it.copy(showError = false) }
        }
    }

    private fun validatePhoneNumber() {
        val phone = _uiState.value.phone
        if (!isValidPhoneNumber(phone)) {
            _uiState.update { it.copy(showError = true) }
        } else {
            viewModelScope.launch {
                userRepository.setPhone(phone)
                val phone = userRepository.getPhone()
                Timber.d("Phone: $phone")
                _effects.emit(PhoneUiEffect.OnNext)
            }
        }
    }

    /**
     * Simple, pragmatic phone validation:
     * - allow leading '+'
     * - digits count between 7 and 15 (ITU E.164 length window)
     * - ignores spaces/dashes for validation
     */
    private fun isValidPhoneNumber(value: String): Boolean {
        if (value.isBlank()) return false
        val cleaned = value.replace(Regex("[\\s-]"), "")
        val regex = Regex("^\\+?\\d{7,15}$")
        return regex.matches(cleaned)
    }
}
