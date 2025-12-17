// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

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
import se.digg.wallet.feature.issuance.KeystoreManager
import timber.log.Timber
import java.util.UUID

sealed interface SetupState {
    object Loading : SetupState
    object Error : SetupState
    object Complete : SetupState
}

class WalletSetupViewModel(private val repo: UserRepository) : ViewModel() {

    val _uiState = MutableStateFlow<SetupState>(SetupState.Loading)
    val uiState: StateFlow<SetupState> = _uiState

    fun requestWua() {
        viewModelScope.launch {
            try {
                val keyPair = KeystoreManager.getOrCreateEs256Key("alias")
                val jwk = KeystoreManager.exportJwk("alias", keyPair)

                val request = WuaRequestModel(
                    walletId = UUID.randomUUID().toString(),
                    jwk = Jwk(
                        kty = jwk.keyType.value,
                        crv = jwk.curve.name,
                        x = jwk.x.toString(),
                        y = jwk.y.toString()
                    )
                )
                val response = RetrofitInstance.api.getWuaRequest(request)
                Timber.d("Wallet setup ok - $response")
                storeWuaLocally(response.jwt)
                _uiState.value = SetupState.Complete

            } catch (e: Exception) {
                Timber.d("Wallet setup error - ${e.message}")
                _uiState.value = SetupState.Error
            }
        }
    }

    fun storeWuaLocally(jwt: String) {
        viewModelScope.launch { repo.setWua(jwt) }
    }

    class Factory(private val appContext: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = DatabaseProvider.get(appContext)
            val repo = UserRepository(db.userDao())
            return WalletSetupViewModel(repo) as T
        }
    }
}