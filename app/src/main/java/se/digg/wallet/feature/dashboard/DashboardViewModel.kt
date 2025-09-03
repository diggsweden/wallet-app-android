package se.digg.wallet.feature.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.json.Json
import se.digg.wallet.core.storage.CredentialStore
import se.digg.wallet.data.CredentialData
import se.digg.wallet.data.CredentialLocal
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date

class DashboardViewModel(app: Application) : AndroidViewModel(app) {

    val credential: StateFlow<CredentialData?> =
        CredentialStore.credentialFlow(context = app)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = null
            )

    val credentialDetails: StateFlow<DashboardCredentialUiModel?> =
        credential.map {
            it?.jwt?.let {
                val credential = Json.decodeFromString(CredentialLocal.serializer(), it)

                DashboardCredentialUiModel(
                    issuer = credential.issuer?.name ?: "",
                    disclosureCount = credential.disclosures.values.size,
                    issueDate = formatDate(credential.issuedAt)
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
    val issueDate: String
)