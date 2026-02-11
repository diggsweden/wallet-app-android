package se.digg.wallet.feature.enrollment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import se.digg.wallet.data.UserRepository

@HiltViewModel
class EnrollmentViewModel @Inject constructor(private val userRepository: UserRepository) :
    ViewModel() {

    private val _uiState = MutableStateFlow(EnrollmentUiState())
    val uiState: StateFlow<EnrollmentUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<EnrollmentUiEvent>()
    val events: SharedFlow<EnrollmentUiEvent> = _events

    var credentialOffer: String = ""

    fun goNext() {
        val currentIndex = _uiState.value.currentStep.ordinal
        if (currentIndex < _uiState.value.totalSteps - 1) {
            _uiState.update {
                it.copy(
                    currentStep = EnrollmentStep.entries.toTypedArray()[
                        currentIndex +
                            1,
                    ],
                )
            }
        }
    }

    fun goBack() {
        val currentIndex = _uiState.value.currentStep.ordinal
        if (currentIndex < _uiState.value.totalSteps - 1) {
            _uiState.update {
                it.copy(
                    currentStep = EnrollmentStep.entries.toTypedArray()[
                        currentIndex -
                            1,
                    ],
                )
            }
        }
    }

    fun onSkip() {
        val currentIndex = _uiState.value.currentStep.ordinal
        if (currentIndex < _uiState.value.totalSteps - 1) {
            _uiState.update {
                it.copy(
                    currentStep = EnrollmentStep.entries.toTypedArray()[
                        currentIndex +
                            2,
                    ],
                )
            }
        }
    }

    fun closeOnboarding() {
        viewModelScope.launch {
            try {
                userRepository.wipeAll()
                _events.emit(EnrollmentUiEvent.LocalStorageCleared)
            } catch (e: Exception) {
                // TODO handle error?
            }
        }
    }

    fun setSessionId(sessionId: String) {
        userRepository.setSessionId(sessionId)
        goNext()
    }

    fun setFetchedCredentialOffer(offer: String) {
        credentialOffer = offer
        goNext()
    }

    fun getFetchedCredentialOffer() {
        viewModelScope.launch {
            _events.emit(EnrollmentUiEvent.CredentialOffer(credentialOffer))
        }
    }
}
