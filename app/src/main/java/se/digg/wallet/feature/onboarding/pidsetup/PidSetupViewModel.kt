// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.onboarding.pidsetup

import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
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
import se.digg.wallet.BuildConfig
import se.digg.wallet.core.di.BaseHttpClient
import se.digg.wallet.core.oauth.LaunchAuthTab
import se.digg.wallet.core.oauth.OAuthCoordinator
import se.digg.wallet.core.oauth.OAuthResult
import se.digg.wallet.data.CredentialsOfferRequestModel
import se.digg.wallet.data.CredentialsOfferResponseModel
import se.digg.wallet.data.UserRepository
import timber.log.Timber

private const val PID_CREDENTIAL_ID = "eu.europa.ec.eudi.pid_vc_sd_jwt"

@HiltViewModel
class PidSetupViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val oAuthCoordinator: OAuthCoordinator,
    @param:BaseHttpClient private val httpClient: HttpClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow<PidSetupUiState>(PidSetupUiState.Idle)
    val uiState: StateFlow<PidSetupUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<PidSetupUiEffect>()
    val effects: SharedFlow<PidSetupUiEffect> = _effects.asSharedFlow()

    val credential =
        userRepository.user
            .map { it?.pid }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun getCredentialOffer(launchAuthTab: LaunchAuthTab) {
        viewModelScope.launch {
            try {
                val credentialOffer =
                    generateCredentialOffer() ?: generateOfferInBrowser(launchAuthTab)

                _effects.emit(
                    PidSetupUiEffect.OnCredentialOfferFetched(credentialOffer = credentialOffer),
                )
            } catch (e: Exception) {
                Timber.d("Credential offer not fetched - ${e.message}")
                _uiState.value = PidSetupUiState.Error
            }
        }
    }

    private suspend fun generateCredentialOffer(): String? = try {
        val url = "https://${BuildConfig.PID_ISSUER_URL}/issuer/credentialsOffer/create"

        val response: CredentialsOfferResponseModel =
            httpClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(CredentialsOfferRequestModel(credentialIds = listOf(PID_CREDENTIAL_ID)))
            }.body()

        response.credentialsOffer
    } catch (e: Exception) {
        Timber.d("generateCredentialOffer failed: ${e.message}")
        null
    }

    private suspend fun generateOfferInBrowser(launchAuthTab: LaunchAuthTab): String = when (
        val oAuthCallback =
            oAuthCoordinator.authorize(
                url = "https://${BuildConfig.PID_ISSUER_URL}".toUri(),
                redirectScheme = "openid-credential-offer",
                launchAuthTab = launchAuthTab,
            )
    ) {
        OAuthResult.Cancelled -> {
            Timber.d("OAuth cancelled")
            _uiState.value = PidSetupUiState.Idle
            throw IllegalStateException("OAuth session cancelled")
        }

        is OAuthResult.Failure -> {
            Timber.d("OAuth failed: ${oAuthCallback.message}")
            _uiState.value = PidSetupUiState.Idle
            throw IllegalStateException("OAuth session failed")
        }

        is OAuthResult.Success -> {
            Timber.d("OAuth Success: ${oAuthCallback.uri}")
            if (oAuthCallback.uri.getQueryParameter("credential_offer") == null) {
                throw IllegalStateException("credential offer query parameter missing")
            }
            oAuthCallback.uri.toString()
        }
    }
}
