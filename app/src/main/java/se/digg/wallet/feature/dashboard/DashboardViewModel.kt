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
import kotlinx.serialization.json.Json
import se.digg.wallet.data.CredentialLocal
import se.digg.wallet.data.UserRepository

@HiltViewModel
class DashboardViewModel @Inject constructor(private val userRepository: UserRepository) :
    ViewModel() {

    val credentialDetails: StateFlow<DashboardCredentialUiModel?> =
        userRepository.user.map {
            it?.credential?.let {
                val credential = Json.decodeFromString(CredentialLocal.serializer(), it)

                DashboardCredentialUiModel(
                    issuer = credential.issuer?.name ?: "",
                    disclosureCount = credential.disclosures.values.size,
                    issueDate = formatDate(credential.issuedAt),
                )
            }
        }
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

fun formatDate(date: Date): String {
    val localDateTime = date.toInstant()
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()

    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
    return localDateTime.format(formatter)
}

data class DashboardCredentialUiModel(
    val issuer: String,
    val disclosureCount: Int,
    val issueDate: String,
)
