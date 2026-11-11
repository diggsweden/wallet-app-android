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
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import se.digg.wallet.core.data.AccountManager

@HiltViewModel
class SettingsViewModel @Inject constructor(private val accountManager: AccountManager) :
    ViewModel() {

    private val _events = MutableSharedFlow<SettingsUiEvent>()
    val events: SharedFlow<SettingsUiEvent> = _events.asSharedFlow()

    fun onLogout() {
        viewModelScope.launch {
            accountManager.wipeAll()
            _events.emit(SettingsUiEvent.LocalStorageCleared)
        }
    }
}
