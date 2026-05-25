// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.onboarding.walletsetup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.ktor.client.HttpClient
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import se.digg.wallet.access_mechanism.api.OpaqueClient
import se.digg.wallet.core.di.GatewayHttpClient
import se.digg.wallet.core.extensions.toECKey
import se.digg.wallet.core.network.WalletOpaqueClient
import se.digg.wallet.core.services.KeyAlias
import se.digg.wallet.core.services.KeystoreManager
import se.digg.wallet.data.UserRepository
import se.wallet.client.gateway.models.CreateAccountRequestDto
import se.wallet.client.gateway.models.KeyRequest
import timber.log.Timber

@HiltViewModel
class WalletSetupViewModel @Inject constructor(
    private val userRepository: UserRepository,
    @param:GatewayHttpClient private val httpClient: HttpClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow<WalletSetupUiState>(
        WalletSetupUiState.InProgress(SetupStep.CREATE_ACCOUNT),
    )
    val uiState: StateFlow<WalletSetupUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<WalletSetupUiEffect>()
    val effects: SharedFlow<WalletSetupUiEffect> = _effects.asSharedFlow()

    private var opaqueApi: OpaqueClient? = null
    private var hsmPublicKey: KeyRequest? = null
    private var pin: String = ""

    fun start(pin: String) {
        this.pin = pin
        runFrom(SetupStep.CREATE_ACCOUNT)
    }

    fun retry() {
        val failedStep = (_uiState.value as? WalletSetupUiState.Failed)?.step ?: return
        runFrom(failedStep)
    }

    private fun runFrom(startStep: SetupStep) {
        viewModelScope.launch(Dispatchers.IO) {
            val steps = SetupStep.entries.dropWhile { it != startStep }
            for (step in steps) {
                _uiState.value = WalletSetupUiState.InProgress(step)
                try {
                    executeStep(step)
                } catch (e: Exception) {
                    Timber.d("WalletSetupViewModel - Step $step failed: ${e.message}")
                    _uiState.value = WalletSetupUiState.Failed(step)
                    return@launch
                }
            }
            _effects.emit(WalletSetupUiEffect.OnNext)
        }
    }

    private suspend fun executeStep(step: SetupStep) {
        when (step) {
            SetupStep.CREATE_ACCOUNT -> createAccount()
            SetupStep.INIT_HSM -> initHsm()
            SetupStep.REGISTER_PIN -> registerPin()
            SetupStep.AUTHENTICATE -> authenticate()
            SetupStep.CREATE_HSM_KEY -> createHsmKey()
            SetupStep.POST_HSM_KEY -> postHsmKey()
        }
    }

    private suspend fun createAccount() {
        val keyPair = KeystoreManager.getOrCreateEs256Key(KeyAlias.WALLET_KEY)
        val ecKey = keyPair.toECKey(withThumbprint = true)
        val accountId = userRepository.createAccount(
            CreateAccountRequestDto(
                personalIdentityNumber = "123456789",
                emailAdress = "test@test.test",
                telephoneNumber = "123456789",
                publicKey = KeyRequest(
                    kty = ecKey.keyType.value,
                    crv = ecKey.curve.name,
                    x = ecKey.x.toString(),
                    y = ecKey.y.toString(),
                    kid = ecKey.keyID,
                ),
            ),
        )
        userRepository.setAccountId(accountId)
        delay(randomDelay())
    }

    private suspend fun initHsm() {
        val keyPair = KeystoreManager.createSoftwareEcdhKey()
        val keyPin = KeystoreManager.getPinStretchPrivateKey()
        opaqueApi = OpaqueClient.create(
            clientKeyPair = keyPair,
            pinStretchPrivateKey = keyPin,
            transport = WalletOpaqueClient(httpClient),
        )
        delay(randomDelay())
    }

    private suspend fun registerPin() {
        checkNotNull(opaqueApi).registration(pin = pin)
        delay(randomDelay())
    }

    private suspend fun authenticate() {
        checkNotNull(opaqueApi).authenticate(pin = pin)
        delay(randomDelay())
    }

    private suspend fun createHsmKey() {
        val response = checkNotNull(opaqueApi).createHsmKey()
        hsmPublicKey =
            json.decodeFromString<CreateHsmKeyResponse>(response).publicKey.toKeyRequest()
        delay(randomDelay())
    }

    private suspend fun postHsmKey() {
        userRepository.postWalletKey(request = checkNotNull(hsmPublicKey))
    }

    fun randomDelay(): Long = Random.nextLong(250L, 800L)
}
