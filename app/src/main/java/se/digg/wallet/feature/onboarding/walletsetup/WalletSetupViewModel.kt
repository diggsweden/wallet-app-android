// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.onboarding.walletsetup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class WalletSetupViewModel @Inject constructor(
    private val walletSetupService: WalletSetupService,
) : ViewModel() {

    private val _uiState = MutableStateFlow<WalletSetupUiState>(
        WalletSetupUiState.InProgress(SetupStep.CREATE_ACCOUNT),
    )
    val uiState: StateFlow<WalletSetupUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<WalletSetupUiEffect>()
    val effects: SharedFlow<WalletSetupUiEffect> = _effects.asSharedFlow()

    private var pin: String = ""

    fun start(pin: String) {
        this.pin = pin
        runFrom(SetupStep.CREATE_ACCOUNT)
    }

    fun retry() {
        val failedStep = (_uiState.value as? WalletSetupUiState.Failed)?.step ?: return
        runFrom(failedStep)
    }

    private fun runFrom(startStep: SetupStep) {
        viewModelScope.launch {
            val steps = SetupStep.entries.dropWhile { it != startStep }
            for (step in steps) {
                _uiState.value = WalletSetupUiState.InProgress(step)
                try {
                    executeStep(step)
                } catch (e: Exception) {
                    Timber.d(e, "WalletSetupViewModel - Step $step failed")
                    _uiState.value = WalletSetupUiState.Failed(step)
                    return@launch
                }
                if (step != SetupStep.entries.last()) {
                    delay(randomDelay())
                }
            }
            _effects.emit(WalletSetupUiEffect.OnNext)
        }
    }

    private suspend fun executeStep(step: SetupStep) {
        when (step) {
            SetupStep.CREATE_ACCOUNT -> walletSetupService.createAccount()
            SetupStep.INIT_HSM -> walletSetupService.initHsm()
            SetupStep.REGISTER_PIN -> walletSetupService.registerPin(pin)
            SetupStep.AUTHENTICATE -> walletSetupService.authenticate(pin)
            SetupStep.POST_HSM_KEY -> walletSetupService.postHsmKey()
        }
    }

    private fun randomDelay(): Long = Random.nextLong(250L, 800L)
}
