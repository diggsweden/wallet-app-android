package se.digg.wallet.feature.enrollment.contactinfo

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import se.digg.wallet.data.CreateAccountRequestDTO
import se.digg.wallet.data.Jwk
import se.digg.wallet.data.UserRepository
import se.digg.wallet.feature.issuance.KeystoreManager
import se.wallet.client.gateway.client.AccountsV1Client
import se.wallet.client.gateway.models.CreateAccountRequestDto
import se.wallet.client.gateway.models.JwkDto
import timber.log.Timber
import javax.inject.Inject

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

@HiltViewModel
class ContactInfoViewModel @Inject constructor(private val userRepository: UserRepository) :
    ViewModel() {

    private val _state = MutableStateFlow(ContactUiState())
    val state: StateFlow<ContactUiState> = _state.asStateFlow()

    private val _done = Channel<Unit>(Channel.BUFFERED)
    val done: Flow<Unit> = _done.receiveAsFlow()

    fun onEvent(event: ContactEvent) {
        when (event) {
            is ContactEvent.PhoneChanged -> updatePhone(event.value)
            is ContactEvent.EmailChanged -> updateEmail(event.value)
            is ContactEvent.VerifyEmailChanged -> updateVerifyEmail(event.value)
            ContactEvent.SubmitClicked -> validateAndSubmit()
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

    private fun validateAndSubmit() {
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
                try {
                    val keyPair = KeystoreManager.getOrCreateEs256Key("alias")
                    val jwk = KeystoreManager.exportJwk("alias", keyPair)

                    val requestBody = CreateAccountRequestDto(
                        personalIdentityNumber = "12345678",
                        emailAdress = "asser@asser.com",
                        telephoneNumber = "0851332391",
                        publicKey = JwkDto(
                            kty = jwk.keyType.value,
                            crv = jwk.curve.name,
                            x = jwk.x.toString(),
                            y = jwk.y.toString(),
                            kid = "myKey"
                        )
                    )
                    val response = userRepository.createAccount(requestBody)
                    val accountId = when (response) {
                        is AccountsV1Client.CreateAccountResult.Failure -> {
                            throw Exception("Kunde inte skapa konto")
                        }

                        is AccountsV1Client.CreateAccountResult.Success -> {
                            response.data.accountId ?: ""
                        }
                    }
                    Timber.d("ContactInfo - Response: $response")
                    userRepository.setAccountId(accountId)
                    _done.send(Unit)
                } catch (e: Exception) {
                    Timber.d("ContactInfo - Account creation error: ${e.message}")
                }
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