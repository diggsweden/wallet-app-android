package se.digg.wallet.feature.enrollment.email

import android.util.Patterns
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
class EmailViewModel @Inject constructor(private val userRepository: UserRepository) :
    ViewModel() {

    private var wasEmailFocused = false
    private var wasVerifyEmailFocused = false
    private var hasNextBeenClicked = false
    private var validateEmailInRealTime = false
    private var validateVerifyEmailInRealTime = false

    private val _uiState = MutableStateFlow(EmailUiState())
    val uiState: StateFlow<EmailUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<EmailUiEffect>()
    val effects: SharedFlow<EmailUiEffect> = _effects.asSharedFlow()

    fun onEvent(event: EmailUiEvent) {
        when (event) {
            is EmailUiEvent.EmailChanged -> updateEmail(event.value)

            is EmailUiEvent.VerifyEmailChanged -> updateVerifyEmail(event.value)

            is EmailUiEvent.EmailFocusedChanged -> checkIfShouldValidateEmail(event.isFocused)

            is EmailUiEvent.VerifyEmailFocusedChanged -> checkIfShouldValidateVerifyEmail(
                event.isFocused,
            )

            EmailUiEvent.NextClicked -> onNextClicked()
        }
    }

    private fun updateEmail(value: String) {
        val value = value.trim()
        _uiState.update { it.copy(email = value) }
        validateInputFields()
    }

    private fun updateVerifyEmail(value: String) {
        val value = value.trim()
        _uiState.update { it.copy(verifyEmail = value) }
        validateInputFields()
    }

    private fun isBothEmailsValid(): Boolean {
        val email = _uiState.value.email
        val verifyEmail = _uiState.value.verifyEmail
        return validateEmail(email) == null && validateEmail(verifyEmail) == null
    }

    private fun isBothEmailsSame(): Boolean {
        val email = _uiState.value.email
        val verifyEmail = _uiState.value.verifyEmail
        return isBothEmailsValid() && email == verifyEmail
    }

    private fun validateInputFields() {
        val uiState = _uiState.value
        val email = uiState.email
        val verifyEmail = uiState.verifyEmail

        if (isBothEmailsValid() && isBothEmailsSame()) {
            _uiState.update {
                it.copy(
                    emailError = null,
                    verifyEmailError = null,
                )
            }
        } else if (isBothEmailsValid() && !isBothEmailsSame()) {
            _uiState.update {
                it.copy(
                    emailError = EmailValidationError.NOT_SAME,
                    verifyEmailError = EmailValidationError.NOT_SAME,
                )
            }
        } else {
            if (wasEmailFocused && validateEmailInRealTime) {
                _uiState.update { it.copy(emailError = validateEmail(email)) }
            }
            if (wasVerifyEmailFocused && validateVerifyEmailInRealTime) {
                _uiState.update { it.copy(verifyEmailError = validateEmail(verifyEmail)) }
            }
        }
    }

    private fun checkIfShouldValidateEmail(focused: Boolean) {
        if (focused) {
            wasEmailFocused = true
        } else if (wasEmailFocused) {
            validateEmailInRealTime = true
            validateInputFields()
        }
    }

    private fun checkIfShouldValidateVerifyEmail(focused: Boolean) {
        if (focused) {
            wasVerifyEmailFocused = true
        } else if (wasVerifyEmailFocused) {
            validateVerifyEmailInRealTime = true
            validateInputFields()
        }
    }

    private fun onNextClicked() {
        hasNextBeenClicked = true
        validateInputFields()
        if (isBothEmailsValid() && isBothEmailsSame()) {
            viewModelScope.launch {
                userRepository.setEmail(_uiState.value.email)
                _effects.emit(EmailUiEffect.OnNext)
            }
        }
    }

    private fun validateEmail(value: String): EmailValidationError? {
        if (value.isBlank()) return EmailValidationError.EMPTY
        return if (Patterns.EMAIL_ADDRESS.matcher(value)
                .matches()
        ) {
            null
        } else {
            EmailValidationError.NOT_VALID_EMAIL
        }
    }
}
