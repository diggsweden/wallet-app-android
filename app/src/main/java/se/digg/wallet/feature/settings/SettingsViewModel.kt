package se.digg.wallet.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import se.digg.wallet.core.storage.user.DatabaseProvider
import se.digg.wallet.core.storage.user.UserRepository

class SettingsViewModel(val userRepository: UserRepository) : ViewModel() {

    sealed interface UiEvent {
        data object LocalStorageCleared : UiEvent
    }
    private val _events = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _events

    fun onLogout() {
        viewModelScope.launch {
            try {
                userRepository.wipeAll()
                _events.emit(UiEvent.LocalStorageCleared)
            } catch (e: Exception) {
                //TODO handle error?
            }
        }
    }

    class Factory(private val appContext: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = DatabaseProvider.get(appContext)
            val repo = UserRepository(db.userDao())
            return SettingsViewModel(repo) as T
        }
    }
}