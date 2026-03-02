package se.digg.wallet.feature.onboarding.fetchid

import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
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
import se.digg.wallet.core.crypto.JwtUtils
import se.digg.wallet.core.di.BaseHttpClient
import se.digg.wallet.core.oauth.LaunchAuthTab
import se.digg.wallet.core.oauth.OAuthCoordinator
import se.digg.wallet.core.oauth.OAuthResult
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
    @BaseHttpClient private val httpClient: HttpClient,
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
                _uiState.value = FetchIdUiState.Error
            }
        }
    }

    fun getCredentialOffer(launchAuthTab: LaunchAuthTab) {
        viewModelScope.launch {
            try {
                val credentialOffer =
                    generateCredentialOffer() ?: generateOfferInBrowser(launchAuthTab)

                _effects.emit(
                    FetchIdUiEffect.OnCredentialOfferFetched(credentialOffer = credentialOffer),
                )
            } catch (e: Exception) {
                Timber.d("Credential offer not fetched - ${e.message}")
                _uiState.value = FetchIdUiState.Error
            }
        }
    }

    private suspend fun generateCredentialOffer(): String? = try {
        val url = "https://${BuildConfig.PID_ISSUER_URL}/issuer/credentialsOffer/generate"
        val body =
            "credentialIds=eu.europa.ec.eudi.pid_vc_sd_jwt&credentialsOfferUri=openid-credential-offer%3A%2F%2F"

        val response =
            httpClient.post(url) {
                contentType(ContentType.parse("application/x-www-form-urlencoded"))
                header(HttpHeaders.Accept, "text/html")
                setBody(body)
            }

        extractCredentialOfferUrl(response.bodyAsText())
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
            _uiState.value = FetchIdUiState.Idle
            throw IllegalStateException("OAuth session cancelled")
        }

        is OAuthResult.Failure -> {
            Timber.d("OAuth failed: ${oAuthCallback.message}")
            _uiState.value = FetchIdUiState.Idle
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

    private fun extractCredentialOfferUrl(html: String): String? {
        val regex = Regex("""openid-credential-offer://[^\s"'<>]+""")
        return regex.find(html)?.value
    }
}
