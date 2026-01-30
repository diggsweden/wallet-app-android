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

enum class AppFlow { Enrollment, Dashboard }

data class AppFlowState(
    val flow: AppFlow = AppFlow.Enrollment,
    val dashboardStartRoute: String? = null,
)

@HiltViewModel
class MainActivityViewModel @Inject constructor(private val userRepository: UserRepository) :
    ViewModel() {
    private val _enrollmentState = MutableStateFlow(AppFlowState())
    val enrollmentState: StateFlow<AppFlowState> = _enrollmentState

    init {
        viewModelScope.launch {
            val credential: String? = userRepository.getCredential()
            if (credential.isNullOrBlank()) {
                goToEnrollment()
            } else {
                goToDashboard()
            }
        }
    }

    fun goToEnrollment() =
        _enrollmentState.update { it.copy(flow = AppFlow.Enrollment, dashboardStartRoute = null) }

    fun goToDashboard(startRoute: String? = null) = _enrollmentState.update {
        it.copy(
            flow = AppFlow.Dashboard,
            dashboardStartRoute = startRoute,
        )
    }
}
