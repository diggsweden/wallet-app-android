package se.digg.wallet

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import se.digg.wallet.core.storage.user.DatabaseProvider
import se.digg.wallet.core.storage.user.UserRepository

enum class AppFlow { Enrollment, Dashboard }

data class AppFlowState(
    val flow: AppFlow = AppFlow.Enrollment,
    val dashboardStartRoute: String? = null
)

class MainActivityViewModel(private val repo: UserRepository) : ViewModel() {
    private val _enrollmentState = MutableStateFlow(AppFlowState())
    val enrollmentState: StateFlow<AppFlowState> = _enrollmentState
    /*val credential = repo.user.map { it?.credential }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
     */

    init {
        viewModelScope.launch {
            val credential: String? = repo.getCredential()
            if (credential.isNullOrBlank()) {
                goToEnrollment()
            } else {
                goToDashboard()
            }
        }
    }

    fun goToEnrollment() =
        _enrollmentState.update { it.copy(flow = AppFlow.Enrollment, dashboardStartRoute = null) }

    fun goToDashboard(startRoute: String? = null) =
        _enrollmentState.update { it.copy(flow = AppFlow.Dashboard, dashboardStartRoute = startRoute) }

    class Factory(private val appContext: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = DatabaseProvider.get(appContext)
            val repo = UserRepository(db.userDao())
            return MainActivityViewModel(repo) as T
        }
    }
}