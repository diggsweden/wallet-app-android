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
import se.digg.wallet.core.trust.TrustStore
import timber.log.Timber

@HiltViewModel
class AppSetupViewModel @Inject constructor(
    private val appSetupService: AppSetupService,
    trustStore: TrustStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AppSetupUiState>(AppSetupUiState.Loading)
    val uiState: StateFlow<AppSetupUiState> = _uiState.asStateFlow()

    private val _effects = Channel<AppSetupUiEffect>(Channel.BUFFERED)
    val effects: Flow<AppSetupUiEffect> = _effects.receiveAsFlow()

    init {
        trustStore.prefetchTrustAnchors()
        resolveStart()
    }

    fun onResetAcknowledged() {
        viewModelScope.launch {
            _effects.send(AppSetupUiEffect.NavigateToIntro)
        }
    }

    fun onRetry() {
        resolveStart()
    }

    private fun resolveStart() {
        _uiState.value = AppSetupUiState.Loading
        viewModelScope.launch {
            try {
                when (appSetupService.resolveStartDestination()) {
                    StartDestination.Intro -> {
                        _effects.send(AppSetupUiEffect.NavigateToIntro)
                    }

                    StartDestination.Dashboard -> {
                        _effects.send(AppSetupUiEffect.NavigateToDashboard)
                    }

                    StartDestination.Reset -> {
                        appSetupService.clearUserData()
                        _uiState.value = AppSetupUiState.Reset
                    }
                }
            } catch (e: Exception) {
                Timber.d("AppSetupViewModel - Start check error: ${e.message}")
                _uiState.value = AppSetupUiState.Error(message = e.message)
            }
        }
    }
}
