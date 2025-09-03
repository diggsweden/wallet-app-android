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

class DashboardViewModel(app: Application) : AndroidViewModel(app) {

    val credential: StateFlow<CredentialData?> =
        CredentialStore.credentialFlow(context = app)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = null
            )

    val disclosureCount: StateFlow<Int?> =
        credential.map {
            it?.jwt?.let {
                val test = Json.decodeFromString(CredentialLocal.serializer(), it)
                test.disclosures.values.size
            }
        }
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000),0)
}