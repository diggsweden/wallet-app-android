package se.digg.wallet.feature.enrollment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import se.digg.wallet.data.UserRepository
import javax.inject.Inject

@HiltViewModel
class EnrollmentViewModel @Inject constructor(private val userRepository: UserRepository) :
    ViewModel() {

    sealed interface UiEvent {
        data object LocalStorageCleared : UiEvent
    }

    private val _uiState = MutableStateFlow(EnrollmentUiState())
    val uiState: StateFlow<EnrollmentUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<EnrollmentViewModel.UiEvent>()
    val events: SharedFlow<EnrollmentViewModel.UiEvent> = _events

    fun goNext() {
        val currentIndex = _uiState.value.currentStep.ordinal
        if (currentIndex < _uiState.value.totalSteps - 1) {
            _uiState.update { it.copy(currentStep = EnrollmentStep.entries.toTypedArray()[currentIndex + 1]) }
        }
    }

    fun goBack() {
        val currentIndex = _uiState.value.currentStep.ordinal
        if (currentIndex < _uiState.value.totalSteps - 1) {
            _uiState.update { it.copy(currentStep = EnrollmentStep.entries.toTypedArray()[currentIndex - 1]) }
        }
    }

    fun onSkip() {
        val currentIndex = _uiState.value.currentStep.ordinal
        if (currentIndex < _uiState.value.totalSteps - 1) {
            _uiState.update { it.copy(currentStep = EnrollmentStep.entries.toTypedArray()[currentIndex + 2]) }
        }
    }

    fun closeOnboarding() {
        viewModelScope.launch {
            try {
                userRepository.wipeAll()
                _events.emit(EnrollmentViewModel.UiEvent.LocalStorageCleared)
            } catch (e: Exception) {
                //TODO handle error?
            }
        }
    }

    fun setSessionId(sessionId: String) {
        userRepository.setSessionId(sessionId)
        goNext()
    }
}

