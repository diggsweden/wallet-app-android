// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.enrollment

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

class OnboardingViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _effects = Channel<OnboardingEffect>(Channel.BUFFERED)
    val effects: Flow<OnboardingEffect> = _effects.receiveAsFlow()

    fun onEvent(event: OnboardingEvent) {
        when (event) {
            OnboardingEvent.TermsCompleted -> TODO()
            OnboardingEvent.ContactInfoCompleted -> TODO()
            OnboardingEvent.Continue -> goNext()
            OnboardingEvent.Back -> goBack()
            OnboardingEvent.Finish -> finish()
        }
    }

    private fun update(block: (OnboardingUiState) -> OnboardingUiState) {
        _uiState.update(block)
    }

    private fun goBack() {
        val current = _uiState.value
        val prev = when (current.step) {
            OnboardingStep.INTRO -> OnboardingStep.INTRO
            //OnboardingStep.TERMS -> OnboardingStep.INTRO
            OnboardingStep.CONTACT -> OnboardingStep.INTRO
            OnboardingStep.PIN -> OnboardingStep.CONTACT
            OnboardingStep.WUA -> OnboardingStep.PIN
            OnboardingStep.DONE -> OnboardingStep.DONE
        }
        update { it.copy(step = prev, error = null) }
        navigateToStep(onboardingStep = prev)
    }

    private fun goNext() {
        val current = _uiState.value
        val next = when (current.step) {
            OnboardingStep.INTRO -> OnboardingStep.CONTACT
            //OnboardingStep.TERMS -> OnboardingStep.CONTACT
            OnboardingStep.CONTACT -> OnboardingStep.PIN
            OnboardingStep.PIN -> OnboardingStep.WUA
            OnboardingStep.WUA -> OnboardingStep.DONE
            OnboardingStep.DONE -> OnboardingStep.DONE
        }
        update { it.copy(step = next, error = null) }
        navigateToStep(onboardingStep = next)
    }

    private fun navigateToStep(onboardingStep: OnboardingStep) {
        viewModelScope.launch { _effects.send(OnboardingEffect.Navigate(onboardingStep.name)) }
    }

    private fun finish() {
        viewModelScope.launch { _effects.send(OnboardingEffect.NavigateToDashboard) }
    }
}