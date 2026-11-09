// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import se.digg.wallet.core.data.CredentialStore
import se.digg.wallet.core.model.SavedCredential

@HiltViewModel
class DashboardViewModel @Inject constructor(private val credentialStore: CredentialStore) :
    ViewModel() {

    val uiState: StateFlow<DashboardUiState> =
        credentialStore.credentials
            .map<List<SavedCredential>, DashboardUiState> { credentials ->
                DashboardUiState.Success(
                    pid = credentials.firstOrNull(),
                    credentials = credentials.drop(1),
                )
            }
            .distinctUntilChanged()
            .catch { emit(DashboardUiState.Error(it.message)) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                DashboardUiState.Loading,
            )
}

sealed interface DashboardUiState {
    data object Loading : DashboardUiState

    data class Success(
        val pid: SavedCredential? = null,
        val credentials: List<SavedCredential> = emptyList(),
    ) : DashboardUiState

    data class Error(val message: String? = null) : DashboardUiState
}
