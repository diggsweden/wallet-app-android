// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import se.digg.wallet.data.SavedCredential
import se.digg.wallet.data.UserRepository

@HiltViewModel
class DashboardViewModel @Inject constructor(private val userRepository: UserRepository) :
    ViewModel() {

    val uiState: StateFlow<DashboardUiModel> =
        userRepository.user.map { user ->
            checkNotNull(user)
            DashboardUiModel(pid = user.pid, credentials = user.credentials)
        }
            .distinctUntilChanged()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                DashboardUiModel(null, emptyList()),
            )
}

fun formatDate(date: Date): String {
    val localDateTime = date.toInstant()
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()

    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
    return localDateTime.format(formatter)
}

data class DashboardUiModel(val pid: SavedCredential?, val credentials: List<SavedCredential>)
