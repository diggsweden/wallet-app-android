// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.appsetup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import se.digg.wallet.data.UserRepository
import timber.log.Timber

@HiltViewModel
class AppSetupViewModel @Inject constructor(private val userRepository: UserRepository) :
    ViewModel() {

    private val _uiState = MutableStateFlow<AppSetupUiState>(AppSetupUiState.Loading)
    val uiState: StateFlow<AppSetupUiState> = _uiState.asStateFlow()

    private val _effects = Channel<AppSetupUiEffect>(Channel.BUFFERED)
    val effects: Flow<AppSetupUiEffect> = _effects.receiveAsFlow()

    init {
        checkIfReset()
    }

    fun onResetAcknowledged() {
        viewModelScope.launch {
            _effects.send(AppSetupUiEffect.NavigateToIntro)
        }
    }

    private fun checkIfReset() {
        _uiState.value = AppSetupUiState.Loading
        viewModelScope.launch {
            try {
                when (isUserReset()) {
                    null -> {
                        _effects.send(AppSetupUiEffect.NavigateToIntro)
                    }

                    true -> {
                        clearUserData()
                        _uiState.value = AppSetupUiState.Reset
                    }

                    false -> {
                        checkIfOnboarded()
                    }
                }
            } catch (e: Exception) {
                Timber.d("AppSetupViewModel - Reset check error: ${e.message}")
                _uiState.value = AppSetupUiState.Error(message = e.message)
            }
        }
    }

    private suspend fun isUserReset(): Boolean? = userRepository.isReset()

    private suspend fun clearUserData() = userRepository.wipeAll()

    private fun checkIfOnboarded() {
        _uiState.value = AppSetupUiState.Loading
        viewModelScope.launch {
            try {
                if (userRepository.isOnboarded()) {
                    checkShouldReset()
                } else {
                    _effects.send(AppSetupUiEffect.NavigateToIntro)
                }
            } catch (e: Exception) {
                Timber.d("AppSetupViewModel - Onboarded check error: ${e.message}")
                _uiState.value = AppSetupUiState.Error(message = e.message)
            }
        }
    }

    private suspend fun checkShouldReset() {
        try {
            val backendGeneration = userRepository.getClientConfig().cacheGeneration
            val localGeneration = userRepository.getBackendGeneration()
            if (localGeneration != null && backendGeneration > localGeneration) {
                clearUserData()
                _uiState.value = AppSetupUiState.Reset
            } else {
                _effects.send(AppSetupUiEffect.NavigateToDashboard)
            }
        } catch (e: Exception) {
            Timber.d("AppSetupViewModel - Setup request error: ${e.message}")
            _uiState.value = AppSetupUiState.Error(message = e.message)
        }
    }
}
