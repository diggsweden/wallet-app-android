package se.digg.wallet.feature.enrollment.activation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import se.digg.wallet.data.Jwk
import se.digg.wallet.data.UserRepository
import se.digg.wallet.data.WuaRequestModel
import se.digg.wallet.feature.issuance.KeystoreManager
import se.wallet.client.gateway.client.WuaClient
import se.wallet.client.gateway.models.CreateWuaDto
import se.wallet.client.gateway.models.JwkDto
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

sealed interface ActivationState {
    object Loading : ActivationState
    object Error : ActivationState
    object Complete : ActivationState
}

@HiltViewModel
class ActivationViewModel @Inject constructor(private val userRepository: UserRepository) :
    ViewModel() {

    val _uiState = MutableStateFlow<ActivationState>(ActivationState.Loading)
    val uiState: StateFlow<ActivationState> = _uiState

    fun requestWua() {
        viewModelScope.launch {
            try {
                val keyPair = KeystoreManager.getOrCreateEs256Key("alias")
                val jwk = KeystoreManager.exportJwk("alias", keyPair)
                val uuid = UUID.randomUUID()
                val request = CreateWuaDto(
                    walletId = uuid.toString(),
                    jwk = JwkDto(
                        kty = jwk.keyType.value,
                        crv = jwk.curve.name,
                        x = jwk.x.toString(),
                        y = jwk.y.toString()
                    )
                )
                val response = userRepository.fetchWua(request)
                val jwt = when (response) {
                    is WuaClient.CreateWuaResult.Failure -> {
                        throw Exception("Could not get WUA")
                    }

                    is WuaClient.CreateWuaResult.Success -> {
                        response.data.jwt ?: ""
                    }
                }
                Timber.d("Wallet activation ok - $response")
                storeWuaLocally(jwt = jwt, uuid = uuid)
                _uiState.value = ActivationState.Complete

            } catch (e: Exception) {
                Timber.d("Wallet activation error - ${e.message}")
                _uiState.value = ActivationState.Error
            }
        }
    }

    fun storeWuaLocally(jwt: String, uuid: UUID) {
        viewModelScope.launch {
            userRepository.setWua(jwt)
            userRepository.setUuid(uuid)
        }
    }
}