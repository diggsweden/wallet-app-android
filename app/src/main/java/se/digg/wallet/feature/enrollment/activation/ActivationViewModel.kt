package se.digg.wallet.feature.enrollment.activation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import se.digg.wallet.core.network.RetrofitInstance
import se.digg.wallet.core.storage.user.DatabaseProvider
import se.digg.wallet.core.storage.user.UserRepository
import se.digg.wallet.data.Jwk
import se.digg.wallet.data.WuaRequestModel
import se.digg.wallet.feature.issuance.KeystoreManager
import timber.log.Timber
import java.util.UUID

sealed interface ActivationState {
    object Loading : ActivationState
    object Error : ActivationState
    object Complete : ActivationState
}

class ActivationViewModel(private val repo: UserRepository) : ViewModel() {

    val _uiState = MutableStateFlow<ActivationState>(ActivationState.Loading)
    val uiState: StateFlow<ActivationState> = _uiState

    fun requestWua() {
        viewModelScope.launch {
            try {
                val keyPair = KeystoreManager.getOrCreateEs256Key("alias")
                val jwk = KeystoreManager.exportJwk("alias", keyPair)
                val uuid = UUID.randomUUID()
                val request = WuaRequestModel(
                    walletId = uuid.toString(),
                    jwk = Jwk(
                        kty = jwk.keyType.value,
                        crv = jwk.curve.name,
                        x = jwk.x.toString(),
                        y = jwk.y.toString()
                    )
                )
                val response = RetrofitInstance.api.getWuaRequest(request)
                Timber.d("Wallet activation ok - $response")
                storeWuaLocally(jwt = response.jwt, uuid = uuid)
                _uiState.value = ActivationState.Complete

            } catch (e: Exception) {
                Timber.d("Wallet activation error - ${e.message}")
                _uiState.value = ActivationState.Error
            }
        }
    }

    fun storeWuaLocally(jwt: String, uuid: UUID) {
        viewModelScope.launch {
            repo.setWua(jwt)
            repo.setUuid(uuid)
        }
    }

    class Factory(private val appContext: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = DatabaseProvider.get(appContext)
            val repo = UserRepository(db.userDao())
            return ActivationViewModel(repo) as T
        }
    }
}