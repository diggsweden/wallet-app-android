package se.digg.wallet.feature.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import se.digg.wallet.core.storage.CredentialStore
import se.digg.wallet.data.CredentialData

class DashboardViewModel(app: Application) : AndroidViewModel(app) {

    val credential: StateFlow<CredentialData?> =
        CredentialStore.credentialFlow(context = app)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = null
            )

}