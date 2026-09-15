// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.issuance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import se.digg.wallet.core.oauth.AuthorizationLauncher
import se.digg.wallet.core.oauth.LaunchAuthTab
import se.digg.wallet.core.oauth.OAuthResult
import se.digg.wallet.data.ClaimUiModel
import se.digg.wallet.data.CredentialStore
import se.digg.wallet.data.IssuerDisplay
import timber.log.Timber

sealed interface IssuanceState {
    val onRetry: (() -> Unit)? get() = null

    data object Loading : IssuanceState
    data class Error(override val onRetry: (() -> Unit)? = null) : IssuanceState
    data class OfferReady(val issuer: IssuerDisplay?) : IssuanceState
    data object AwaitingPin : IssuanceState
    data object ReadyToFetch : IssuanceState
    data class CredentialIssued(val issuer: IssuerDisplay?, val claims: List<ClaimUiModel>) :
        IssuanceState
}

@HiltViewModel
class IssuanceViewModel @Inject constructor(
    private val issuanceService: IssuanceService,
    private val authorizationLauncher: AuthorizationLauncher,
    private val credentialStore: CredentialStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow<IssuanceState>(IssuanceState.Loading)
    val uiState: StateFlow<IssuanceState> = _uiState

    fun fetchIssuer(uri: String) {
        _uiState.value = IssuanceState.Loading
        viewModelScope.launch {
            try {
                _uiState.value = IssuanceState.OfferReady(issuanceService.fetchOffer(uri))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Timber.d(e, "IssuanceViewModel: Fetch issuer error")
                _uiState.value = IssuanceState.Error(onRetry = { fetchIssuer(uri) })
            }
        }
    }

    fun authorize(launchAuthTab: LaunchAuthTab) {
        val offerReady = _uiState.value as? IssuanceState.OfferReady ?: return
        viewModelScope.launch {
            try {
                val result = authorizationLauncher.authorize(
                    url = issuanceService.authorizationUrl(),
                    redirectScheme = "wallet-app",
                    launchAuthTab = launchAuthTab,
                )
                check(result is OAuthResult.Success) {
                    "Authorization did not complete: $result"
                }
                issuanceService.exchangeAuthorizationCode(result.redirectUri)
                _uiState.value = IssuanceState.AwaitingPin
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Timber.d(e, "IssuanceViewModel: Authorize error")
                _uiState.value = IssuanceState.Error(onRetry = { _uiState.value = offerReady })
            }
        }
    }

    fun createProof(pin: String) {
        if (_uiState.value != IssuanceState.AwaitingPin) return
        viewModelScope.launch {
            _uiState.value = IssuanceState.Loading
            try {
                issuanceService.createProof(pin)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Timber.d(e, "IssuanceViewModel: Create proof error")
                _uiState.value = IssuanceState.Error(
                    onRetry = { _uiState.value = IssuanceState.AwaitingPin },
                )
                return@launch
            }
            _uiState.value = IssuanceState.ReadyToFetch
        }
    }

    fun fetchCredential() {
        if (_uiState.value != IssuanceState.ReadyToFetch) return
        viewModelScope.launch {
            _uiState.value = IssuanceState.Loading
            try {
                val issued = issuanceService.fetchCredential()
                credentialStore.addCredentials(listOf(issued.credential))
                _uiState.value = IssuanceState.CredentialIssued(
                    issuer = issued.credential.issuer,
                    claims = issued.claims,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Timber.d(e, "IssuanceViewModel: Fetch credential error")
                _uiState.value = IssuanceState.Error(
                    onRetry = { _uiState.value = IssuanceState.ReadyToFetch },
                )
            }
        }
    }

    fun retry() {
        _uiState.value.onRetry?.invoke()
    }
}
