// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.appsetup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
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
        setup()
    }

    fun onResetAcknowledged() {
        viewModelScope.launch {
            _effects.send(AppSetupUiEffect.NavigateToIntro)
        }
    }

    fun onRetry() {
        setup()
    }

    private fun setup() {
        _uiState.value = AppSetupUiState.Loading
        viewModelScope.launch {
            try {
                resetOrNavigate()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.d("AppSetupViewModel - Setup error: ${e.message}")
                _uiState.value = AppSetupUiState.Error(message = e.message)
            }
        }
    }

    private suspend fun resetOrNavigate() {
        val user = userRepository.getUser() ?: return goToIntro()

        if (user.isReset) {
            return resetUser()
        }

        if (!user.isOnboarded) {
            return goToIntro()
        }

        if (checkForBackendReset(localGeneration = user.backendGeneration)) {
            return resetUser()
        }

        goToDashboard()
    }

    private suspend fun goToIntro() = _effects.send(AppSetupUiEffect.NavigateToIntro)

    private suspend fun goToDashboard() = _effects.send(AppSetupUiEffect.NavigateToDashboard)

    private suspend fun resetUser() {
        userRepository.wipeAll()
        _uiState.value = AppSetupUiState.Reset
    }

    private suspend fun checkForBackendReset(localGeneration: Int?): Boolean {
        val backendGeneration = userRepository.getClientConfig().cacheGeneration

        if (localGeneration == null) {
            userRepository.setBackendGeneration(backendGeneration)
            return false
        }

        return backendGeneration > localGeneration
    }
}
