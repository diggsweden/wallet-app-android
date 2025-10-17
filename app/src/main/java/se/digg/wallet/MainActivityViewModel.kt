package se.digg.wallet

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import se.digg.wallet.core.storage.user.DatabaseProvider
import se.digg.wallet.core.storage.user.UserRepository

enum class AppFlow { Onboarding, Dashboard, PIN }

data class AppFlowState(
    val flow: AppFlow = AppFlow.Onboarding,
    val dashboardStartRoute: String? = null
)

class MainActivityViewModel(private val repo: UserRepository) : ViewModel() {
    private val _state = MutableStateFlow(AppFlowState())
    val state: StateFlow<AppFlowState> = _state
    val pin = repo.user.map { it?.pin }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch {
            val pin: String? = repo.getPin()
            if(pin.isNullOrBlank()){
                goToOnboarding()
            } else
            {
                goToDashboard()
            }
        }
    }

    fun goToOnboarding() =
        _state.update { it.copy(flow = AppFlow.Onboarding, dashboardStartRoute = null) }

    fun goToDashboard(startRoute: String? = null) =
        _state.update { it.copy(flow = AppFlow.Dashboard, dashboardStartRoute = startRoute) }

    fun goToPinInput(startRoute: String? = null) =
        _state.update { it.copy(flow = AppFlow.PIN, dashboardStartRoute = startRoute) }

    class Factory(private val appContext: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = DatabaseProvider.get(appContext)
            val repo = UserRepository(db.userDao())
            return MainActivityViewModel(repo) as T
        }
    }
}