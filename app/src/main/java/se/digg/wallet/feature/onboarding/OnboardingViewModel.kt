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

    var credentialOffer: String = ""

    fun goNext() {
        val currentIndex = _uiState.value.currentStep.ordinal
        if (currentIndex < _uiState.value.totalSteps - 1) {
            _uiState.update {
                it.copy(
                    currentStep = OnboardingStep.entries.toTypedArray()[
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
                    currentStep = OnboardingStep.entries.toTypedArray()[
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
                    currentStep = OnboardingStep.entries.toTypedArray()[
                        currentIndex +
                            2,
                    ],
                )
            }
        }
    }

    fun closeOnboarding() {
        viewModelScope.launch {
            userRepository.wipeAll()
            _events.emit(OnboardingUiEvent.LocalStorageCleared)
        }
    }

    fun setFetchedCredentialOffer(offer: String) {
        credentialOffer = offer
        goNext()
    }

    fun getCredentialOfferUrl(): String = credentialOffer
}
