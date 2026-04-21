// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import se.digg.wallet.data.UserRepository
import timber.log.Timber

@HiltViewModel
class SettingsViewModel @Inject constructor(private val userRepository: UserRepository) :
    ViewModel() {

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
                // TODO handle error?
            }
        }
    }
}
