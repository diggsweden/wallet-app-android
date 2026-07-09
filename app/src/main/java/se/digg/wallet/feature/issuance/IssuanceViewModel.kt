// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.issuance

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import se.digg.wallet.core.oauth.AuthorizationLauncher
import se.digg.wallet.core.oauth.LaunchAuthTab
import se.digg.wallet.core.oauth.OAuthResult
import se.digg.wallet.core.passkey.PasskeyAssertResult
import se.digg.wallet.core.passkey.PasskeyConfirmUiState
import se.digg.wallet.core.passkey.PasskeyManager
import se.digg.wallet.core.services.KeystoreManager
import se.digg.wallet.data.ClaimUiModel
import se.digg.wallet.data.CredentialStore
import se.digg.wallet.data.IssuerDisplay
import se.digg.wallet.data.PasskeyStore
import timber.log.Timber

enum class IssuanceRetryStep { FetchOffer, Authorize, CreateProof, FetchCredential, SaveCredential }

sealed interface IssuanceState {
    data object Loading : IssuanceState
    data class Error(val retryStep: IssuanceRetryStep) : IssuanceState
    data class OfferReady(val issuer: IssuerDisplay?) : IssuanceState
    data object AwaitingPin : IssuanceState
    data class CredentialIssued(val issuer: IssuerDisplay?, val claims: List<ClaimUiModel>) :
        IssuanceState
}

@HiltViewModel
class IssuanceViewModel @Inject constructor(
    private val issuanceService: IssuanceService,
    private val authorizationLauncher: AuthorizationLauncher,
    private val credentialStore: CredentialStore,
    private val passkeyStore: PasskeyStore,
    private val passkeyManager: PasskeyManager,
) : ViewModel() {
    private val _uiState = MutableStateFlow<IssuanceState>(IssuanceState.Loading)
    val uiState: StateFlow<IssuanceState> = _uiState.asStateFlow()

    private var offerUri: String? = null
    private var issuerDisplay: IssuerDisplay? = null
    private var issuedCredential: IssuedCredential? = null
    private var operation: Job? = null

    private val _passkeyConfirm = MutableStateFlow(PasskeyConfirmUiState())
    val passkeyConfirm: StateFlow<PasskeyConfirmUiState> = _passkeyConfirm.asStateFlow()

    init {
        viewModelScope.launch {
            _passkeyConfirm.update { it.copy(passkey = passkeyStore.getPasskey()) }
        }
    }

    /**
     * Passkey PoC: replaces the PIN entry at the signing step. A successful
     * assertion releases the locally wrapped PIN, which still drives the
     * OPAQUE authentication in [createProof].
     */
    fun createProofWithPasskey(activityContext: Context) {
        val passkey = _passkeyConfirm.value.passkey ?: return
        if (_passkeyConfirm.value.inProgress) return
        viewModelScope.launch {
            _passkeyConfirm.update { it.copy(inProgress = true, error = null) }
            when (val result = passkeyManager.assertPasskey(activityContext, passkey)) {
                PasskeyAssertResult.Success -> {
                    val encryptedPin = passkeyStore.getEncryptedPin()
                    if (encryptedPin == null) {
                        _passkeyConfirm.update {
                            it.copy(inProgress = false, error = "No stored PIN")
                        }
                    } else {
                        _passkeyConfirm.update { it.copy(inProgress = false) }
                        createProof(KeystoreManager.decryptPin(encryptedPin))
                    }
                }

                PasskeyAssertResult.Cancelled -> {
                    _passkeyConfirm.update { it.copy(inProgress = false) }
                }

                is PasskeyAssertResult.Failed -> {
                    _passkeyConfirm.update {
                        it.copy(inProgress = false, error = result.message)
                    }
                }
            }
        }
    }

    fun fetchIssuer(uri: String) {
        if (operation?.isActive == true) return
        offerUri = uri
        issuerDisplay = null
        issuedCredential = null
        _uiState.value = IssuanceState.Loading
        operation = viewModelScope.launch {
            try {
                issuerDisplay = issuanceService.fetchOffer(uri)
                _uiState.value = IssuanceState.OfferReady(issuerDisplay)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showError("Fetch issuer error", e, IssuanceRetryStep.FetchOffer)
            }
        }
    }

    fun authorize(launchAuthTab: LaunchAuthTab) {
        if (_uiState.value !is IssuanceState.OfferReady) return
        _uiState.value = IssuanceState.Loading
        operation = viewModelScope.launch {
            try {
                val result = authorizationLauncher.authorize(
                    url = issuanceService.authorizationUrl(),
                    redirectScheme = "wallet-app",
                    launchAuthTab = launchAuthTab,
                )
                when (result) {
                    is OAuthResult.Success -> {
                        issuanceService.exchangeAuthorizationCode(result.redirectUri)
                        _uiState.value = IssuanceState.AwaitingPin
                    }

                    OAuthResult.Cancelled -> {
                        _uiState.value = IssuanceState.OfferReady(issuerDisplay)
                    }

                    is OAuthResult.Failure -> {
                        showError(
                            "Authorize error",
                            IllegalStateException(result.message),
                            IssuanceRetryStep.Authorize,
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showError("Authorize error", e, IssuanceRetryStep.Authorize)
            }
        }
    }

    fun createProof(pin: String) {
        if (_uiState.value != IssuanceState.AwaitingPin) return
        _uiState.value = IssuanceState.Loading
        operation = viewModelScope.launch {
            try {
                issuanceService.createProof(pin)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showError("Create proof error", e, IssuanceRetryStep.CreateProof)
                return@launch
            }
            fetchCredential()
        }
    }

    fun retry() {
        val retryStep = (_uiState.value as? IssuanceState.Error)?.retryStep ?: return
        when (retryStep) {
            IssuanceRetryStep.FetchOffer -> {
                fetchIssuer(checkNotNull(offerUri))
            }

            IssuanceRetryStep.Authorize -> {
                _uiState.value = IssuanceState.OfferReady(issuerDisplay)
            }

            IssuanceRetryStep.CreateProof -> {
                _uiState.value = IssuanceState.AwaitingPin
            }

            IssuanceRetryStep.FetchCredential -> {
                _uiState.value = IssuanceState.Loading
                operation = viewModelScope.launch { fetchCredential() }
            }

            IssuanceRetryStep.SaveCredential -> {
                _uiState.value = IssuanceState.Loading
                operation = viewModelScope.launch { saveCredential() }
            }
        }
    }

    private suspend fun fetchCredential() {
        try {
            issuedCredential = issuanceService.fetchCredential()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            showError("Fetch credential error", e, IssuanceRetryStep.FetchCredential)
            return
        }
        saveCredential()
    }

    private suspend fun saveCredential() {
        val issued = checkNotNull(issuedCredential)
        try {
            credentialStore.addCredentials(listOf(issued.credential))
            _uiState.value = IssuanceState.CredentialIssued(
                issuer = issued.credential.issuer,
                claims = issued.claims,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            showError("Save credential error", e, IssuanceRetryStep.SaveCredential)
        }
    }

    private fun showError(message: String, cause: Exception, retryStep: IssuanceRetryStep) {
        Timber.d(cause, "IssuanceViewModel: $message")
        _uiState.value = IssuanceState.Error(retryStep)
    }
}
