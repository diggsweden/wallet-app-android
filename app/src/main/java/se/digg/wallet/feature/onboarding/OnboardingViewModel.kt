// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.onboarding

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
class OnboardingViewModel @Inject constructor(private val userRepository: UserRepository) :
    ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<OnboardingUiEvent>()
    val events: SharedFlow<OnboardingUiEvent> = _events

    private var credentialOffer: String = ""

    fun onAction(action: OnboardingAction) {
        when (action) {
            is OnboardingAction.Next -> {
                goNext(action.fromStep)
            }

            is OnboardingAction.Back -> {
                goBack(action.fromStep)
            }

            OnboardingAction.Skip -> {
                goSkip()
            }

            OnboardingAction.Finish -> {
                /* handled by UI layer via event */
            }

            OnboardingAction.Close -> {
                closeOnboarding()
            }

            is OnboardingAction.CredentialOfferFetched -> {
                ifCurrent(action.fromStep) {
                    credentialOffer = action.url
                    goNext(action.fromStep)
                }
            }

            is OnboardingAction.PinEntered -> {
                ifCurrent(action.fromStep) {
                    _uiState.update { it.copy(capturedPin = action.pin) }
                    goNext(action.fromStep)
                }
            }

            is OnboardingAction.PinVerified -> {
                ifCurrent(action.fromStep) {
                    if (_uiState.value.capturedPin == action.pin) {
                        goNext(action.fromStep)
                    } else {
                        goBack(action.fromStep)
                    }
                }
            }
        }
    }

    fun getCredentialOfferUrl(): String = credentialOffer

    private inline fun ifCurrent(fromStep: OnboardingStep, block: () -> Unit) {
        if (_uiState.value.currentStep == fromStep) block()
    }

    private fun goNext(fromStep: OnboardingStep) {
        _uiState.update { state ->
            if (state.currentStep == fromStep && fromStep.ordinal < state.totalSteps - 1) {
                state.copy(currentStep = OnboardingStep.entries[fromStep.ordinal + 1])
            } else {
                state
            }
        }
    }

    private fun goBack(fromStep: OnboardingStep) {
        _uiState.update { state ->
            if (state.currentStep == fromStep && fromStep.ordinal > 0) {
                state.copy(currentStep = OnboardingStep.entries[fromStep.ordinal - 1])
            } else {
                state
            }
        }
    }

    private fun goSkip() {
        val currentIndex = _uiState.value.currentStep.ordinal
        if (currentIndex < _uiState.value.totalSteps - 1) {
            _uiState.update {
                it.copy(
                    currentStep = OnboardingStep.entries.toTypedArray()[currentIndex + 2],
                )
            }
        }
    }

    private fun closeOnboarding() {
        viewModelScope.launch {
            userRepository.wipeAll()
            _events.emit(OnboardingUiEvent.LocalStorageCleared)
        }
    }
}
