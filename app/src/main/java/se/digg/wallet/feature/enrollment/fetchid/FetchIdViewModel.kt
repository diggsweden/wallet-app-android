// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.enrollment.fetchid

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
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
import se.digg.wallet.core.oauth.LaunchAuthTab
import se.digg.wallet.core.oauth.OAuthCoordinator
import se.digg.wallet.core.services.KeyAlias
import se.digg.wallet.core.services.KeystoreManager
import se.digg.wallet.data.UserRepository
import se.wallet.client.gateway.client.NetworkResult
import se.wallet.client.gateway.models.CreateAccountRequestDto
import se.wallet.client.gateway.models.JwkDto
import timber.log.Timber

@HiltViewModel
class FetchIdViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val oAuthCoordinator: OAuthCoordinator,
) : ViewModel() {
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
                val keyPair = KeystoreManager.getOrCreateEs256Key(KeyAlias.WALLET_KEY)
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
                        is NetworkResult.Failure -> {
                            throw Exception("Kunde inte skapa konto")
                        }

                        is NetworkResult.Success -> {
                            response.data.accountId ?: ""
                        }
                    }
                Timber.d("ContactInfo - Response: $response")
                userRepository.setAccountId(accountId)
                _uiState.value = FetchIdUiState.Idle
            } catch (e: Exception) {
                Timber.d("ContactInfo - Account creation error: ${e.message}")
            }
        }
    }

    fun getCredentialOffer(launchAuthTab: LaunchAuthTab) {
        viewModelScope.launch {
            val oAuthCallback = oAuthCoordinator.authorize(
                url = "https://wallet.sandbox.digg.se/pid-issuer".toUri(),
                redirectScheme = "openid-credential-offer",
                launchAuthTab = launchAuthTab,
            )
            val credentialOffer =
                oAuthCallback.getQueryParameter("credential_offer")
                    ?: throw IllegalStateException("credential offer query parameter missing")

            val encodedJson = Uri.encode(credentialOffer)
            val uri = Uri.parse(
                "openid-credential-offer://?credential_offer=$encodedJson",
            ).toString()
            _effects.emit(
                FetchIdUiEffect.OnCredentialOfferFetched(credentialOffer = uri),
            )
        }
    }
}
