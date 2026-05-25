// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import se.digg.wallet.data.UserRepository

enum class AppFlow { Onboarding, Dashboard }

data class AppFlowState(
    val flow: AppFlow = AppFlow.Onboarding,
    val dashboardStartRoute: String? = null,
)

@HiltViewModel
class MainActivityViewModel @Inject constructor(private val userRepository: UserRepository) :
    ViewModel() {
    private val _onboardingState = MutableStateFlow(AppFlowState())
    val onboardingState: StateFlow<AppFlowState> = _onboardingState

    init {
        viewModelScope.launch {
            val credential = userRepository.getPid()
            if (credential == null) {
                goToOnboarding()
            } else {
                goToDashboard()
            }
        }
    }

    fun goToOnboarding() {
        _onboardingState.update { it.copy(flow = AppFlow.Onboarding, dashboardStartRoute = null) }
    }

    fun goToDashboard(startRoute: String? = null) = _onboardingState.update {
        it.copy(
            flow = AppFlow.Dashboard,
            dashboardStartRoute = startRoute,
        )
    }
}
