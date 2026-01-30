package se.digg.wallet.feature.enrollment.fetchid

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import se.digg.wallet.core.crypto.JwtUtils
import se.digg.wallet.core.services.KeyAlias
import se.digg.wallet.core.services.KeystoreManager
import se.digg.wallet.data.UserRepository
import se.wallet.client.gateway.client.OidcAccountsV1Client
import se.wallet.client.gateway.client.WuaV3Client.CreateWua_1Result
import se.wallet.client.gateway.models.CreateAccountRequestDto
import se.wallet.client.gateway.models.CreateWuaDto
import se.wallet.client.gateway.models.JwkDto
import timber.log.Timber

@HiltViewModel
class FetchIdViewModel
@Inject
constructor(private val userRepository: UserRepository) :
    ViewModel() {
    init {
        setupAccount()
    }

    private val _uiState = MutableStateFlow<FetchIdUiState>(FetchIdUiState.Loading)
    val uiState: StateFlow<FetchIdUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<FetchIdUiEffect>()
    val effects: SharedFlow<FetchIdUiEffect> = _effects.asSharedFlow()

    val credential =
        userRepository.user
            .map { it?.credential }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setupAccount() {
        viewModelScope.launch {
            try {
                val keyPair = KeystoreManager.getOrCreateEs256Key(KeyAlias.DEVICE_KEY)
                val jwk = JwtUtils.exportJwk(keyPair)
                val email = userRepository.getEmail() ?: ""
                val phone = userRepository.getPhone() ?: ""

                val requestBody =
                    CreateAccountRequestDto(
                        personalIdentityNumber = "12345678",
                        emailAdress = email,
                        telephoneNumber = phone,
                        publicKey =
                            JwkDto(
                                kty = jwk.keyType.value,
                                crv = jwk.curve.name,
                                x = jwk.x.toString(),
                                y = jwk.y.toString(),
                                kid = jwk.keyID,
                            ),
                    )
                val response = userRepository.createAccount(requestBody)
                val accountId =
                    when (response) {
                        is OidcAccountsV1Client.CreateAccountResult.Failure -> {
                            throw Exception("Kunde inte skapa konto")
                        }

                        is OidcAccountsV1Client.CreateAccountResult.Success -> {
                            response.data.accountId ?: ""
                        }
                    }
                Timber.d("ContactInfo - Response: $response")
                userRepository.setAccountId(accountId)
                requestWua()
            } catch (e: Exception) {
                Timber.d("ContactInfo - Account creation error: ${e.message}")
            }
        }
    }

    fun requestWua() {
        viewModelScope.launch {
            try {
                val keyPair = KeystoreManager.getOrCreateEs256Key(KeyAlias.WALLET_KEY)
                val jwk = JwtUtils.exportJwk(keyPair)
                val uuid = UUID.randomUUID()
                val request =
                    CreateWuaDto(
                        walletId = uuid.toString(),
                        jwk =
                            JwkDto(
                                kty = jwk.keyType.value,
                                crv = jwk.curve.name,
                                x = jwk.x.toString(),
                                y = jwk.y.toString(),
                            ),
                    )
                val response = userRepository.fetchWua(request)
                val jwt =
                    when (response) {
                        is CreateWua_1Result.Failure -> {
                            throw Exception("Could not get WUA")
                        }

                        is CreateWua_1Result.Success -> {
                            response.data.jwt ?: ""
                        }
                    }
                Timber.d("Wallet activation ok - $response")
                storeWuaLocally(jwt = jwt, uuid = uuid)
                _uiState.value = FetchIdUiState.Idle
            } catch (e: Exception) {
                Timber.d("Wallet activation error - ${e.message}")
                _uiState.value = FetchIdUiState.Error
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
