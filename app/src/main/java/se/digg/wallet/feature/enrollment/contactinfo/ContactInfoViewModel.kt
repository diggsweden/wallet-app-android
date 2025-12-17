// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.enrollment.contactinfo

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ContactUiState(
    val phone: String = "",
    val email: String = "",
    val verifyEmail: String = "",
    val phoneError: String? = null,
    val emailError: String? = null,
    val verifyEmailError: String? = null,
    val isSubmitting: Boolean = false
) {
    val isValid: Boolean
        get() = phoneError == null
                && emailError == null
                && verifyEmailError == null
                && phone.isNotBlank()
                && email.isNotBlank()
                && verifyEmail.isNotBlank()
                && email == verifyEmail
}

sealed interface ContactEvent {
    data class PhoneChanged(val value: String) : ContactEvent
    data class EmailChanged(val value: String) : ContactEvent
    data class VerifyEmailChanged(val value: String) : ContactEvent
    data object SubmitClicked : ContactEvent
}

class ContactInfoViewModel : ViewModel() {
    private val _state = MutableStateFlow(ContactUiState())
    val state: StateFlow<ContactUiState> = _state.asStateFlow()

    // one-shot navigation/finish event
    private val _done = Channel<Unit>(Channel.BUFFERED)
    val done: Flow<Unit> = _done.receiveAsFlow()

    fun onEvent(event: ContactEvent) {
        when (event) {
            is ContactEvent.PhoneChanged -> updatePhone(event.value)
            is ContactEvent.EmailChanged -> updateEmail(event.value)
            is ContactEvent.VerifyEmailChanged -> updateVerifyEmail(event.value)
            ContactEvent.SubmitClicked -> submit()
        }
    }

    private fun updatePhone(raw: String) {
        val value = raw.trim()
        val error = validatePhone(value)
        _state.update { it.copy(phone = value, phoneError = error) }
    }

    private fun updateEmail(raw: String) {
        val value = raw.trim()
        val error = validateEmail(value)
        _state.update { it.copy(email = value, emailError = error) }
    }

    private fun updateVerifyEmail(raw: String) {
        val value = raw.trim()
        val error = validateEmail(value)
        _state.update { it.copy(verifyEmail = value, verifyEmailError = error) }
    }

    private fun submit() {
        val s = _state.value
        val phoneErr = validatePhone(s.phone)
        val emailErr = validateEmail(s.email)
        val verifyEmailErr = validateEmail(s.verifyEmail)
        _state.update {
            it.copy(
                phoneError = phoneErr,
                emailError = emailErr,
                verifyEmailError = verifyEmailErr
            )
        }

        if (phoneErr == null && emailErr == null && verifyEmailErr == null) {
            viewModelScope.launch {
                _done.send(Unit)
            }
        }
    }

    private fun validateEmail(value: String): String? {
        if (value.isBlank()) return "E-post är obligatoriskt"
        return if (Patterns.EMAIL_ADDRESS.matcher(value).matches()) null else "Ange en valid e-post"
    }

    /**
     * Simple, pragmatic phone validation:
     * - allow leading '+'
     * - digits count between 7 and 15 (ITU E.164 length window)
     * - ignores spaces/dashes for validation
     */
    private fun validatePhone(value: String): String? {
        if (value.isBlank()) return "Telefonnummer är obligatoriskt"
        val cleaned = value.replace(Regex("[\\s-]"), "")
        val regex = Regex("^\\+?\\d{7,15}$")
        return if (regex.matches(cleaned)) null else "Ange ett giltigt telefonnummer"
    }
}