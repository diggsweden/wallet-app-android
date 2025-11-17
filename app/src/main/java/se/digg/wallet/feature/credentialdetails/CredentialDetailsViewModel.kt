package se.digg.wallet.feature.credentialdetails

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import se.digg.wallet.core.storage.user.DatabaseProvider
import se.digg.wallet.core.storage.user.UserRepository
import se.digg.wallet.data.CredentialLocal
import se.digg.wallet.data.DisclosureLocal
import timber.log.Timber

sealed interface CredentialDetailsState {
    object Loading : CredentialDetailsState
    data class Disclosures(val disclosures: List<DisclosureLocal>, val issuer: String?) :
        CredentialDetailsState

    data class Error(val errorMessage: String) : CredentialDetailsState
}

class CredentialDetailsViewModel(val userRepository: UserRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<CredentialDetailsState>(CredentialDetailsState.Loading)
    val uiState: StateFlow<CredentialDetailsState> = _uiState

    fun matchDisclosures() {
        viewModelScope.launch {
            try {
                val storedCredential = userRepository.getCredential()
                val credential: CredentialLocal = storedCredential?.let {
                    return@let Json.decodeFromString(CredentialLocal.serializer(), it)
                } ?: return@launch
                val disclosures = credential.disclosures.values.toList()
                _uiState.value = CredentialDetailsState.Disclosures(
                    disclosures = disclosures,
                    issuer = credential.issuer?.name
                )
            } catch (e: Exception) {
                Timber.d("CredentialDetailsViewModel - Error: ${e.message}")
                _uiState.value =
                    CredentialDetailsState.Error(errorMessage = "Error with loading locally stored PID - Error: ${e.message}")
            }
        }
    }

    class Factory(private val appContext: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = DatabaseProvider.get(appContext)
            val repo = UserRepository(db.userDao())
            return CredentialDetailsViewModel(repo) as T
        }
    }
}